package dev.typicalfarmingmacro.mixin;

import dev.typicalfarmingmacro.proxy.TfmProxy;
import dev.typicalfarmingmacro.proxy.TfmProxyManager;
import io.netty.channel.Channel;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.Connection$1")
public final class MixinConnectionChannelInitializer {
    @Inject(method = "initChannel(Lio/netty/channel/Channel;)V", at = @At("HEAD"))
    private void typicalfarmingmacro$addProxyHandler(Channel channel, CallbackInfo ci) {
        TfmProxy proxy = TfmProxyManager.selectedProxy();
        if (proxy == null) {
            TfmProxyManager.markConnectionProxy(null);
            return;
        }

        if (proxy.type() == TfmProxy.ProxyType.SOCKS5) {
            channel.pipeline().addFirst(new Socks5ProxyHandler(
                    proxy.socketAddress(),
                    proxy.username().isEmpty() ? null : proxy.username(),
                    proxy.password().isEmpty() ? null : proxy.password()));
        } else {
            channel.pipeline().addFirst(new Socks4ProxyHandler(
                    proxy.socketAddress(),
                    proxy.username().isEmpty() ? null : proxy.username()));
        }
        TfmProxyManager.markConnectionProxy(proxy);
    }
}
