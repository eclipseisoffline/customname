package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.text.Text;

public record ParsedPlayerName(String name, Text parsed) {
    public static final Codec<ParsedPlayerName> CODEC = Codec.STRING.comapFlatMap(raw -> {
        try {
            return DataResult.success(new ParsedPlayerName(raw, CustomName.argumentToText(raw, true, false, false)));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Failed to parse player name: " + exception.getMessage());
        }
    }, ParsedPlayerName::name);
}
