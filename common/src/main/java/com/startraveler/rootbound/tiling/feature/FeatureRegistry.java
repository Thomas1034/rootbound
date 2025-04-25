package com.startraveler.rootbound.tiling.feature;

import com.startraveler.rootbound.Constants;
import com.startraveler.rootbound.registration.RegistrationProvider;
import com.startraveler.rootbound.registration.RegistryObject;
import com.startraveler.rootbound.tiling.feature.custom.ChessboardFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class FeatureRegistry {

    public static final RegistrationProvider<Feature<?>> FEATURES = RegistrationProvider.get(
            Registries.FEATURE,
            Constants.MOD_ID
    );


    public static final RegistryObject<Feature<?>, Feature<NoneFeatureConfiguration>> CHESSBOARD = FEATURES.register("chessboard",
            () -> new ChessboardFeature(NoneFeatureConfiguration.CODEC)
    );

    public static void init() {

    }

}
