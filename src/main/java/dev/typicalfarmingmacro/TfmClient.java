package dev.typicalfarmingmacro;

import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.feature.ClientFeatureBootstrap;
import dev.typicalfarmingmacro.feature.LiveTfmBootstrapHooks;
import dev.typicalfarmingmacro.proxy.TfmProxyManager;
import dev.typicalfarmingmacro.renderer.TfmRenderQueue;
import dev.typicalfarmingmacro.renderer.NanoVGManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class TfmClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TfmConfig.init();
        TfmProxyManager.init();
        TfmBootstrapHooks.install(new LiveTfmBootstrapHooks());
        ClientFeatureBootstrap.initialize();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientFeatureBootstrap.shutdown());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TfmConfig.flush());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            TfmBootstrapHooks.reset();
            TfmRenderQueue.clear();
            if (NanoVGManager.isInitialized()) {
                NanoVGManager.destroy();
            }
        });
    }
}
