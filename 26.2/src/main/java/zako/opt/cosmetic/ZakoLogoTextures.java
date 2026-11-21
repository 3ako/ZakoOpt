package zako.opt.cosmetic;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class ZakoLogoTextures {
    private static final Map<Integer, Identifier> CACHE = new HashMap<>();
    private static final String ROOT = "/assets/zakoopt/textures/cosmetic/";

    private ZakoLogoTextures() {}

    public static Identifier get(int logo, boolean white) {
        int key = logo * 2 + (white ? 1 : 0);
        return CACHE.computeIfAbsent(key, ignored -> create(logo, white));
    }

    private static Identifier create(int logo, boolean white) {
        String name = switch (logo) {
            case 4 -> "horizon";
            case 5 -> "vkusno";
            case 6 -> "rostics";
            default -> throw new IllegalArgumentException("Unknown logo: " + logo);
        };
        Identifier fallback = Identifier.fromNamespaceAndPath("zakoopt", "textures/cosmetic/zako" + (white ? "_white" : "") + ".png");
        try (var shirtStream = ZakoLogoTextures.class.getResourceAsStream(ROOT + "zako" + (white ? "_white" : "") + ".png");
             var logoStream = ZakoLogoTextures.class.getResourceAsStream(ROOT + "logo_" + name + ".png")) {
            if (shirtStream == null || logoStream == null) throw new IOException("Missing cosmetic texture");
            BufferedImage shirt = ImageIO.read(shirtStream);
            BufferedImage mark = ImageIO.read(logoStream);
            if (shirt == null || mark == null) throw new IOException("Invalid cosmetic texture");
            Graphics2D graphics = shirt.createGraphics();
            try {
                // The source patch is plain fabric beside the existing shirt print in the atlas.
                graphics.setComposite(AlphaComposite.Src);
                graphics.drawImage(shirt.getSubimage(1160, 1095, 220, 220), 1426, 1095, null);
                graphics.setComposite(AlphaComposite.SrcOver);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.drawImage(mark, 1426, 1095, 220, 220, null);
            } finally {
                graphics.dispose();
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(shirt, "PNG", png);
            NativeImage pixels = NativeImage.read(png.toByteArray());
            Identifier id = Identifier.fromNamespaceAndPath("zakoopt", "dynamic/zako_" + name + (white ? "_white" : "_black"));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Zako " + name, pixels));
            return id;
        } catch (Exception e) {
            LoggerFactory.getLogger("zakoopt").error("Could not create Zako logo texture", e);
            return fallback;
        }
    }
}
