package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.ui.settings.RangeSliderSetting;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

final class FarmingSettingsFactory {
    private FarmingSettingsFactory() {}

    static java.util.List<String> sprayMaterials() {
        // Include an explicit "Use selected" option as the first choice so users can
        // opt to let the sprayonator use whatever material is currently selected.
        return java.util.List.of("Use Selected", "Compost", "Honey Jar", "Dung", "Plant Matter",
                "Tasty Cheese", "Jelly");
    }

    private static RangeSliderSetting intDelayRangeSetting(String name,
                                                           float minBound,
                                                           float maxBound,
                                                           java.util.function.Supplier<Integer> minGetter,
                                                           java.util.function.Supplier<Integer> maxGetter,
                                                           java.util.function.BiConsumer<Integer, Integer> setter) {
        return new RangeSliderSetting(name, minBound, maxBound,
                () -> minGetter.get().floatValue(),
                () -> maxGetter.get().floatValue(),
                (lower, upper) -> {
                    setter.accept(Math.round(lower), Math.round(upper));
                    TfmConfig.save();
                })
                .withDecimals(0).withSuffix("ms");
    }

    static RangeSliderSetting laneSwitchDelaySetting() {
        return intDelayRangeSetting("Lane Switch Delay", 0f, 1000f,
                () -> TfmConfig.MACRO_LANE_SWITCH_DELAY_MIN.get(),
                () -> TfmConfig.MACRO_LANE_SWITCH_DELAY_MAX.get(),
                (min, max) -> {
                    TfmConfig.MACRO_LANE_SWITCH_DELAY_MIN.set(min);
                    TfmConfig.MACRO_LANE_SWITCH_DELAY_MAX.set(max);
                });
    }

    static RangeSliderSetting rewarpDelaySetting() {
        return intDelayRangeSetting("Rewarp Delay", 0f, 1000f,
                () -> TfmConfig.REWARP_DELAY_MIN.get(),
                () -> TfmConfig.REWARP_DELAY_MAX.get(),
                (min, max) -> {
                    TfmConfig.REWARP_DELAY_MIN.set(min);
                    TfmConfig.REWARP_DELAY_MAX.set(max);
                });
    }

    static RangeSliderSetting pestDestroyerTriggerDelaySetting() {
        return intDelayRangeSetting("Pest Destroyer Trigger Delay", 0f, 5000f,
                () -> TfmConfig.PEST_CHAT_TRIGGER_DELAY_MIN.get(),
                () -> TfmConfig.PEST_CHAT_TRIGGER_DELAY_MAX.get(),
                (min, max) -> {
                    TfmConfig.PEST_CHAT_TRIGGER_DELAY_MIN.set(min);
                    TfmConfig.PEST_CHAT_TRIGGER_DELAY_MAX.set(max);
                });
    }

    static RangeSliderSetting pestExchangeDelaySetting() {
        return intDelayRangeSetting("Pest Exchange Delay", 0f, 5000f,
                () -> TfmConfig.PEST_EXCHANGE_DELAY_MIN.get(),
                () -> TfmConfig.PEST_EXCHANGE_DELAY_MAX.get(),
                (min, max) -> {
                    TfmConfig.PEST_EXCHANGE_DELAY_MIN.set(min);
                    TfmConfig.PEST_EXCHANGE_DELAY_MAX.set(max);
                });
    }

    static RangeSliderSetting aotvBetweenPestsDelaySetting() {
        return intDelayRangeSetting("AOTV Between Pests Delay", 100f, 250f,
                () -> TfmConfig.PEST_AOTV_DELAY_MIN.get(),
                () -> TfmConfig.PEST_AOTV_DELAY_MAX.get(),
                (min, max) -> {
                    TfmConfig.PEST_AOTV_DELAY_MIN.set(min);
                    TfmConfig.PEST_AOTV_DELAY_MAX.set(max);
                });
    }

    static RangeSliderSetting rodSwapDelaySetting() {
        return intDelayRangeSetting("Rod Swap Delay", 0f, 1000f,
                () -> TfmConfig.ROD_SWAP_DELAY_MIN.get(),
                () -> TfmConfig.ROD_SWAP_DELAY_MAX.get(),
                (min, max) -> {
                    TfmConfig.ROD_SWAP_DELAY_MIN.set(min);
                    TfmConfig.ROD_SWAP_DELAY_MAX.set(max);
                });
    }

    static RangeSliderSetting guiFirstClickDelaySetting() {
        return intDelayRangeSetting("GUI First Click Delay", 0f, 1000f,
                () -> TfmConfig.GUI_FIRST_CLICK_DELAY_MIN.get(),
                () -> TfmConfig.GUI_FIRST_CLICK_DELAY_MAX.get(),
                (min, max) -> {
                    TfmConfig.GUI_FIRST_CLICK_DELAY_MIN.set(min);
                    TfmConfig.GUI_FIRST_CLICK_DELAY_MAX.set(max);
                });
    }

    static RangeSliderSetting guiClickDelaySetting() {
        return intDelayRangeSetting("Gear Swap GUI Delay", 0f, 1000f,
                () -> TfmConfig.GUI_CLICK_DELAY_MIN.get(),
                () -> TfmConfig.GUI_CLICK_DELAY_MAX.get(),
                (min, max) -> {
                    TfmConfig.GUI_CLICK_DELAY_MIN.set(min);
                    TfmConfig.GUI_CLICK_DELAY_MAX.set(max);
                });
    }

    static RangeSliderSetting pickUpStashDelaySetting() {
        return intDelayRangeSetting("Pick Up Stash Delay", 0f, 5000f,
                () -> TfmConfig.PICK_UP_STASH_DELAY_MIN.get(),
                () -> TfmConfig.PICK_UP_STASH_DELAY_MAX.get(),
                (min, max) -> {
                    TfmConfig.PICK_UP_STASH_DELAY_MIN.set(min);
                    TfmConfig.PICK_UP_STASH_DELAY_MAX.set(max);
                });
    }

    static RangeSliderSetting junkDropDelaySetting() {
        return intDelayRangeSetting("Junk Drop Delay", 0f, 1000f,
                () -> TfmConfig.JUNK_ITEM_DROP_DELAY_MIN.get(),
                () -> TfmConfig.JUNK_ITEM_DROP_DELAY_MAX.get(),
                (min, max) -> {
                    TfmConfig.JUNK_ITEM_DROP_DELAY_MIN.set(min);
                    TfmConfig.JUNK_ITEM_DROP_DELAY_MAX.set(max);
                });
    }

    static RangeSliderSetting georgePostSellDelaySetting() {
        return intDelayRangeSetting("George Sell Delay Between Pets", 0f, 5000f,
                () -> TfmConfig.GEORGE_POST_SELL_DELAY_MIN_MS.get(),
                () -> TfmConfig.GEORGE_POST_SELL_DELAY_MAX_MS.get(),
                (min, max) -> {
                    TfmConfig.GEORGE_POST_SELL_DELAY_MIN_MS.set(min);
                    TfmConfig.GEORGE_POST_SELL_DELAY_MAX_MS.set(max);
                });
    }

    static ToggleSetting farmWhileCallingGeorgeSetting() {
        return new ToggleSetting("Farm while calling George",
                () -> TfmConfig.FARM_WHILE_CALLING_GEORGE.get(),
                v -> {
                    TfmConfig.FARM_WHILE_CALLING_GEORGE.set(v);
                    TfmConfig.save();
                });
    }

    static RangeSliderSetting bazaarGuiDelaySetting() {
        return intDelayRangeSetting("Bazaar GUI Delay", 0f, 1000f,
                () -> TfmConfig.BAZAAR_DELAY_MIN.get(),
                () -> TfmConfig.BAZAAR_DELAY_MAX.get(),
                (min, max) -> {
                    TfmConfig.BAZAAR_DELAY_MIN.set(min);
                    TfmConfig.BAZAAR_DELAY_MAX.set(max);
                });
    }

    static SliderSetting farmingPitchRangeSetting() {
        return new SliderSetting("Farming Pitch Range", 0, 10,
                () -> TfmConfig.MACRO_CUSTOM_PITCH_HUMANIZATION.get(),
                v -> {
                    TfmConfig.MACRO_CUSTOM_PITCH_HUMANIZATION.set(v);
                    TfmConfig.save();
                })
                .withDecimals(1).withSuffix("\u00B0");
    }

    static SliderSetting farmingYawRangeSetting() {
        return new SliderSetting("Farming Yaw Range", 0, 10,
                () -> TfmConfig.MACRO_CUSTOM_YAW_HUMANIZATION.get(),
                v -> {
                    TfmConfig.MACRO_CUSTOM_YAW_HUMANIZATION.set(v);
                    TfmConfig.save();
                })
                .withDecimals(1).withSuffix("\u00B0");
    }

    static SliderSetting bpsAverageWindowSetting() {
        return new SliderSetting("BPS Average Window", 5, 60,
                () -> (float) TfmConfig.BPS_AVERAGE_WINDOW.get(),
                v -> {
                    TfmConfig.BPS_AVERAGE_WINDOW.set(Math.round(v));
                    TfmConfig.save();
                })
                .withDecimals(0).withSuffix("s");
    }

    static SliderSetting aotvToRoofPitchRangeSetting() {
        return new SliderSetting("AOTV to Roof Pitch Range", 0, 15,
                () -> (float) TfmConfig.AOTV_ROOF_PITCH_HUMANIZATION.get(),
                v -> {
                    TfmConfig.AOTV_ROOF_PITCH_HUMANIZATION.set(Math.round(v));
                    TfmConfig.save();
                })
                .withDecimals(0).withSuffix("\u00B0");
    }

    static SliderSetting pestFovRangeSetting() {
        return new SliderSetting("Pest FOV Range", 0, 90,
                () -> TfmConfig.PEST_FOV_RANGE.get(),
                v -> {
                    TfmConfig.PEST_FOV_RANGE.set(v);
                    TfmConfig.save();
                })
                .withDecimals(1).withSuffix("\u00B0");
    }

    static SliderSetting visitorFovRangeSetting() {
        return new SliderSetting("Visitor FOV Range", 0, 30,
                () -> TfmConfig.VISITOR_FOV_RANGE.get(),
                v -> {
                    TfmConfig.VISITOR_FOV_RANGE.set(v);
                    TfmConfig.save();
                })
                .withDecimals(1).withSuffix("\u00B0");
    }

    static SliderSetting pestExchangeFovRangeSetting() {
        return new SliderSetting("Phillip FOV Range", 0, 15,
                () -> TfmConfig.PEST_EXCHANGE_FOV_RANGE.get(),
                v -> {
                    TfmConfig.PEST_EXCHANGE_FOV_RANGE.set(v);
                    TfmConfig.save();
                })
                .withDecimals(1).withSuffix("\u00B0");
    }

    static RangeSliderSetting pestAboveAimPitchRangeSetting() {
        return new RangeSliderSetting("Pest Above Aim Pitch", 10, 90,
                () -> TfmConfig.PEST_ABOVE_TARGET_PITCH_MIN.get(),
                () -> TfmConfig.PEST_ABOVE_TARGET_PITCH_MAX.get(),
                (lower, upper) -> {
                    TfmConfig.PEST_ABOVE_TARGET_PITCH_MIN.set(lower);
                    TfmConfig.PEST_ABOVE_TARGET_PITCH_MAX.set(upper);
                    TfmConfig.save();
                })
                .withDecimals(0).withSuffix("\u00B0");
    }
}
