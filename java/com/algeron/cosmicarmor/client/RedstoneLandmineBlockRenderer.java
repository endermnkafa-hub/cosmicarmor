package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.block.RedstoneLandmineBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class RedstoneLandmineBlockRenderer extends GeoBlockRenderer<RedstoneLandmineBlockEntity> {
    public RedstoneLandmineBlockRenderer() {
        super(new RedstoneLandmineModel());
    }
}
