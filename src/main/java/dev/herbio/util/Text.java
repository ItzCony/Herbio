package dev.herbio.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/** MiniMessage helpers; item text is rendered non-italic by default. */
public final class Text {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Text() {
    }

    public static Component of(String input, TagResolver... resolvers) {
        return MINI_MESSAGE.deserialize(input, resolvers);
    }

    public static Component item(String input, TagResolver... resolvers) {
        return of(input, resolvers).decoration(TextDecoration.ITALIC, false);
    }
}
