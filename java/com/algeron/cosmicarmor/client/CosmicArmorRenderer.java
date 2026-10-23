package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.item.CosmicArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class CosmicArmorRenderer extends GeoArmorRenderer<CosmicArmorItem> {
    private static final String LEGGINGS_BODY_BONE = "armorLeggingsBody";
    private static final String WAIST_BONE = "armorWaist";

    private final boolean leggingsItem;

    private static final class MotionState {
        double lastX;
        double lastY;
        double lastZ;
        float lastYaw;
        float scrollU = 0.0f;
        float scrollV = 0.0f;
        float velU = 0.0f;
        float velV = 0.0f;
        long lastTimeMs = 0L;
        boolean initialized = false;
    }

    private final Map<Integer, MotionState> states = new HashMap<>();
    private float currentRenderOffsetU = 0.0f;
    private float currentRenderOffsetV = 0.0f;

    public CosmicArmorRenderer(boolean leggingsItem) {
        super(new CosmicArmorModel());
        this.leggingsItem = leggingsItem;
    }

    private void forWaistBone(Consumer<GeoBone> action) {
        this.model.getBone(LEGGINGS_BODY_BONE).ifPresent(action);
        if (this.model.getBone(LEGGINGS_BODY_BONE).isEmpty()) {
            this.model.getBone(WAIST_BONE).ifPresent(action);
        }
    }

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        super.applyBaseTransformations(baseModel);

        // The waistband is a pants piece but follows the torso's position and rotation,
        // so it stays attached to the waist instead of swinging with either leg.
        if (this.leggingsItem && baseModel != null) {
            forWaistBone(waist -> {
                RenderUtils.matchModelPartRot(baseModel.body, waist);
                waist.updatePosition(baseModel.body.x, -baseModel.body.y, baseModel.body.z);
            });
        }
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        super.applyBoneVisibilityBySlot(currentSlot);

        // Show the extra waist bone only when the leggings are the item being rendered.
        forWaistBone(waist -> {
            boolean shouldShow = this.leggingsItem && currentSlot == EquipmentSlot.LEGS;
            waist.setHidden(!shouldShow);
        });
    }

    /**
     * Updates sub-pixel floating-point scroll offsets based on:
     * 1. Player movement direction (strafing right vs strafing left, walking forward vs backward)
     * 2. Camera viewing angle and perspective
     * Provides liquid smooth momentum and halts when stationary.
     */
    private void updateOffsets(Entity entity) {
        if (entity == null) {
            this.currentRenderOffsetU = 0.0f;
            this.currentRenderOffsetV = 0.0f;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();

        MotionState state = this.states.computeIfAbsent(entity.getId(), k -> new MotionState());

        if (!state.initialized) {
            state.lastX = entity.getX();
            state.lastY = entity.getY();
            state.lastZ = entity.getZ();
            state.lastYaw = entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot();
            state.lastTimeMs = now;
            state.initialized = true;
        }

        float dt = (now - state.lastTimeMs) / 1000.0f;
        state.lastTimeMs = now;
        if (dt <= 0.0f || dt > 0.1f) {
            dt = 0.016f;
        }

        float targetVelU = 0.0f;
        float targetVelV = 0.0f;

        // Tuned speeds: gentle, elegant cosmic drift instead of rushing too fast
        final float STRAFE_SPEED = 0.045f;
        final float FORWARD_SPEED = 0.035f;
        final float CAM_YAW_SPEED = 0.0006f;
        final float PERSPECTIVE_STRENGTH = 0.20f;

        if (mc.screen != null) {
            // Inventory / GUI: gentle movement when mouse turns character
            float currentYaw = entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot();
            float yawDiff = Mth.wrapDegrees(currentYaw - state.lastYaw);
            state.lastYaw = currentYaw;
            targetVelU = yawDiff * 0.0018f;
        } else {
            // World movement: compute local strafe (right/left) and forward (front/back)
            double dx = entity.getX() - state.lastX;
            double dy = entity.getY() - state.lastY;
            double dz = entity.getZ() - state.lastZ;
            state.lastX = entity.getX();
            state.lastY = entity.getY();
            state.lastZ = entity.getZ();

            float bodyYaw = entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot();
            double yawRad = Math.toRadians(bodyYaw);

            double rightX = Math.cos(yawRad);
            double rightZ = Math.sin(yawRad);
            double forwardX = -Math.sin(yawRad);
            double forwardZ = Math.cos(yawRad);

            // localStrafe: positive when moving RIGHT, negative when moving LEFT
            double localStrafe = dx * rightX + dz * rightZ;
            // localForward: positive when moving FORWARD, negative when moving BACKWARD
            double localForward = dx * forwardX + dz * forwardZ;

            // When moving right: slides right; when moving left: slides left (at calm cosmic speed)
            targetVelU += (float) (localStrafe * STRAFE_SPEED);
            targetVelV += (float) (localForward * FORWARD_SPEED);

            // Gentle camera turning response when viewing local player
            if (entity == mc.player && mc.gameRenderer != null && mc.gameRenderer.getMainCamera() != null) {
                Camera camera = mc.gameRenderer.getMainCamera();
                float camYaw = camera.getYRot();
                float camYawDiff = Mth.wrapDegrees(camYaw - state.lastYaw);
                state.lastYaw = camYaw;
                targetVelU += camYawDiff * CAM_YAW_SPEED;
            } else {
                state.lastYaw = bodyYaw;
            }
        }

        // Smooth liquid acceleration and damping
        float blend = Math.min(1.0f, dt * 8.0f);
        state.velU = Mth.lerp(blend, state.velU, targetVelU);
        state.velV = Mth.lerp(blend, state.velV, targetVelV);

        // Smooth decay when stopping so it glides to a stop cleanly
        if (Math.abs(targetVelU) < 0.0001f) {
            state.velU *= 0.85f;
            if (Math.abs(state.velU) < 0.00001f) state.velU = 0.0f;
        }
        if (Math.abs(targetVelV) < 0.0001f) {
            state.velV *= 0.85f;
            if (Math.abs(state.velV) < 0.00001f) state.velV = 0.0f;
        }

        // Accumulate sub-pixel smooth scroll offset
        state.scrollU = (state.scrollU + state.velU) % 1.0f;
        state.scrollV = (state.scrollV + state.velV) % 1.0f;

        // Subtle perspective depth based on viewing angle
        float perspectiveU = 0.0f;
        float perspectiveV = 0.0f;
        if (mc.screen == null && mc.gameRenderer != null && mc.gameRenderer.getMainCamera() != null) {
            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 camPos = camera.getPosition();
            if (entity == mc.player) {
                perspectiveU = (camera.getYRot() / 360.0f) * PERSPECTIVE_STRENGTH;
                perspectiveV = (camera.getXRot() / 180.0f) * (PERSPECTIVE_STRENGTH * 0.5f);
            } else {
                double cdx = camPos.x - entity.getX();
                double cdz = camPos.z - entity.getZ();
                if (cdx * cdx + cdz * cdz > 0.001) {
                    double angleToCam = Math.toDegrees(Math.atan2(cdz, cdx)) - 90.0;
                    float bodyYaw = entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot();
                    perspectiveU = (float) (((angleToCam - bodyYaw) / 360.0) * PERSPECTIVE_STRENGTH);
                }
            }
        }

        this.currentRenderOffsetU = state.scrollU + perspectiveU;
        this.currentRenderOffsetV = state.scrollV + perspectiveV;

        // Periodic map cleanup if many entities were rendered
        if (this.states.size() > 100) {
            this.states.entrySet().removeIf(e -> now - e.getValue().lastTimeMs > 10000L);
        }
    }

    @Override
    public void prepForRender(Entity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> baseModel) {
        super.prepForRender(entity, stack, slot, baseModel);
        updateOffsets(entity);
    }

    @Override
    public void preRender(PoseStack poseStack, CosmicArmorItem animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        updateOffsets(this.currentEntity);
    }

    @Override
    public void updateAnimatedTextureFrame(CosmicArmorItem animatable) {
        // Disabled tick-based texture animation; dynamic continuous sub-pixel scrolling is handled in createVerticesOfQuad
    }

    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseMatrix, Vector3f normalMatrix,
                                     VertexConsumer buffer, int packedLight, int packedOverlay,
                                     float red, float green, float blue, float alpha) {
        float uOffset = this.currentRenderOffsetU;
        float vOffset = this.currentRenderOffsetV;
        for (GeoVertex vertex : quad.vertices()) {
            Vector4f pos = poseMatrix.transform(new Vector4f(
                    vertex.position().x(),
                    vertex.position().y(),
                    vertex.position().z(),
                    1.0f));
            float u = vertex.texU() + uOffset;
            float v = vertex.texV() + vOffset;
            buffer.vertex(pos.x(), pos.y(), pos.z(), red, green, blue, alpha, u, v,
                    packedOverlay, packedLight, normalMatrix.x(), normalMatrix.y(), normalMatrix.z());
        }
    }
}
