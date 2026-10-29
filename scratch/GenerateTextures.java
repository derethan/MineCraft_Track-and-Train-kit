import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateTextures {
    public static void main(String[] args) throws Exception {
        generateEntityTexture();
        generateGuiTexture();
        generateItemTexture();
        System.out.println("Textures successfully generated!");
    }

    private static void generateEntityTexture() throws Exception {
        int w = 128;
        int h = 128;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Base industrial steel tone
        Color darkSteel = new Color(0x2D, 0x31, 0x38);
        Color midSteel = new Color(0x3B, 0x40, 0x4B);
        Color lightSteel = new Color(0x56, 0x5D, 0x6C);
        Color borderSteel = new Color(0x1E, 0x21, 0x26);
        Color hazardYellow = new Color(0xF5, 0xB0, 0x1A);
        Color hazardBlack = new Color(0x1B, 0x1D, 0x20);
        Color chrome = new Color(0x9E, 0xA8, 0xB6);
        Color rustRed = new Color(0xA3, 0x36, 0x28);
        Color seatBrown = new Color(0x52, 0x34, 0x23);
        Color lightCyan = new Color(0x6A, 0xEB, 0xF5);

        // Fill background with subtle dark industrial noise
        g.setColor(darkSteel);
        g.fillRect(0, 0, w, h);

        // 1. Monolith Hull (0,0 to 128,35)
        for (int y = 0; y < 36; y++) {
            for (int x = 0; x < w; x++) {
                int noise = ((x * 13 + y * 29) % 7) - 3;
                int r = Math.min(255, Math.max(0, midSteel.getRed() + noise));
                int gc = Math.min(255, Math.max(0, midSteel.getGreen() + noise));
                int b = Math.min(255, Math.max(0, midSteel.getBlue() + noise));
                img.setRGB(x, y, new Color(r, gc, b).getRGB());
            }
        }

        // Add hazard warning stripes across front/rear bumper zones (36, 20 to 66, 32)
        for (int x = 36; x < 66; x++) {
            for (int y = 20; y < 32; y++) {
                boolean stripe = ((x + y) / 3) % 2 == 0;
                img.setRGB(x, y, (stripe ? hazardYellow : hazardBlack).getRGB());
            }
        }

        // Coupler hitches (70, 20 to 86, 32)
        g.setColor(borderSteel);
        g.fillRect(70, 20, 16, 12);
        g.setColor(chrome);
        g.drawRect(71, 21, 14, 10);

        // 2. Cockpit (0,36 to 90,52)
        g.setColor(darkSteel);
        g.fillRect(0, 36, 90, 16);
        // Roof plate (8, 36 to 45, 52)
        g.setColor(midSteel);
        g.fillRect(8, 36, 38, 16);
        g.setColor(borderSteel);
        g.drawRect(8, 36, 37, 15);
        // Operator Seat (74, 36 to 88, 52)
        g.setColor(seatBrown);
        g.fillRect(74, 36, 14, 16);
        g.setColor(new Color(0x3B, 0x24, 0x16));
        g.drawRect(74, 36, 13, 15);

        // 3. Track Laying Chute (0, 53 to 64, 69)
        g.setColor(midSteel);
        g.fillRect(0, 53, 28, 16);
        // Add guide tooth details
        g.setColor(lightSteel);
        g.fillRect(2, 55, 24, 4);
        g.setColor(borderSteel);
        g.drawRect(0, 53, 27, 15);
        // Rollers & Shoes (28, 53 to 55, 69)
        g.setColor(chrome);
        g.fillRect(28, 53, 27, 16);
        g.setColor(borderSteel);
        g.drawRect(28, 53, 26, 15);
        // Floodlights (56, 53 to 64, 61)
        g.setColor(borderSteel);
        g.fillRect(56, 53, 8, 8);
        g.setColor(lightCyan);
        g.fillRect(58, 55, 4, 4);

        // 4. Track Retriever Arm (0, 70 to 64, 86)
        g.setColor(midSteel);
        g.fillRect(0, 70, 28, 16);
        g.setColor(borderSteel);
        g.drawRect(0, 70, 27, 15);
        // Hydraulic cylinders (28, 70 to 41, 86)
        g.setColor(rustRed);
        g.fillRect(28, 70, 14, 16);
        g.setColor(chrome);
        g.fillRect(32, 73, 6, 10);
        // Rear roller (42, 70 to 64, 86)
        g.setColor(chrome);
        g.fillRect(42, 70, 22, 16);

        // 5. Engine Unit (0, 87 to 41, 105)
        g.setColor(darkSteel);
        g.fillRect(0, 87, 22, 18);
        // Radiator cooling louvers
        g.setColor(hazardBlack);
        for (int ly = 89; ly < 103; ly += 2) {
            g.drawLine(2, ly, 18, ly);
        }
        // Exhaust pipe (22, 87 to 41, 105)
        g.setColor(borderSteel);
        g.fillRect(22, 87, 19, 18);
        g.setColor(chrome);
        g.fillRect(24, 89, 4, 14);

        // 6. Running Gear: Wheels & Axles (42, 87 to 80, 115)
        g.setColor(chrome);
        g.fillRect(42, 87, 10, 28);
        g.setColor(borderSteel);
        g.drawRect(42, 87, 9, 27);
        // Axles (52, 87 to 80, 115)
        g.setColor(lightSteel);
        g.fillRect(52, 87, 28, 28);
        g.setColor(borderSteel);
        g.drawRect(52, 87, 27, 27);

        g.dispose();

        File out = new File("src/main/resources/assets/miningtools/textures/entity/track_layer_cart.png");
        out.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", out);
    }

    private static void generateGuiTexture() throws Exception {
        int w = 256;
        int h = 256;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        int frameW = 176;
        int frameH = 204;

        Color bg = new Color(0xC6, 0xC6, 0xC6);
        Color darkBorder = new Color(0x37, 0x37, 0x37);
        Color lightBorder = new Color(0xFF, 0xFF, 0xFF);
        Color slotBg = new Color(0x8B, 0x8B, 0x8B);

        // Frame body
        g.setColor(bg);
        g.fillRect(0, 0, frameW, frameH);

        // 3D Borders of container window
        g.setColor(lightBorder);
        g.drawLine(0, 0, frameW - 1, 0);
        g.drawLine(0, 0, 0, frameH - 1);
        g.setColor(darkBorder);
        g.drawLine(frameW - 1, 0, frameW - 1, frameH - 1);
        g.drawLine(0, frameH - 1, frameW - 1, frameH - 1);

        // Slot drawer helper
        java.util.function.BiConsumer<Integer, Integer> drawSlot = (sx, sy) -> {
            g.setColor(darkBorder);
            g.drawLine(sx, sy, sx + 17, sy);
            g.drawLine(sx, sy, sx, sy + 17);
            g.setColor(lightBorder);
            g.drawLine(sx + 17, sy, sx + 17, sy + 17);
            g.drawLine(sx, sy + 17, sx + 17, sy + 17);
            g.setColor(slotBg);
            g.fillRect(sx + 1, sy + 1, 16, 16);
        };

        // 1. Pattern slot at (18, 20)
        drawSlot.accept(18, 20);

        // 2. Storage grid 3x9 at (8 + col*18, 54 + row*18)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot.accept(8 + col * 18, 54 + row * 18);
            }
        }

        // 3. Player inventory 3x9 at (8 + col*18, 122 + row*18)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot.accept(8 + col * 18, 122 + row * 18);
            }
        }

        // 4. Hotbar 1x9 at (8 + col*18, 180)
        for (int col = 0; col < 9; col++) {
            drawSlot.accept(8 + col * 18, 180);
        }

        g.dispose();

        File out = new File("src/main/resources/assets/miningtools/textures/gui/track_layer_cart.png");
        out.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", out);
    }

    private static void generateItemTexture() throws Exception {
        int s = 16;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        Color body = new Color(0x2D, 0x31, 0x38);
        Color bodyLight = new Color(0x4E, 0x55, 0x64);
        Color hazard = new Color(0xF5, 0xB0, 0x1A);
        Color railMetal = new Color(0xCF, 0xD6, 0xDF);
        Color railWood = new Color(0x65, 0x48, 0x2A);
        Color wheel = new Color(0x1B, 0x1D, 0x20);

        // Chassis body
        g.setColor(body);
        g.fillRect(2, 6, 12, 6);
        g.setColor(bodyLight);
        g.fillRect(3, 7, 10, 2);

        // Rollcage / Canopy
        g.setColor(bodyLight);
        g.drawRect(4, 2, 8, 4);

        // Front track dispenser chute
        g.setColor(hazard);
        g.fillRect(1, 9, 2, 3);

        // Rear retriever arm
        g.setColor(new Color(0xB8, 0x32, 0x32));
        g.fillRect(13, 8, 2, 4);

        // Mini track underneath
        g.setColor(railWood);
        g.fillRect(0, 14, 16, 2);
        g.setColor(railMetal);
        g.drawLine(0, 13, 15, 13);

        // Wheels
        g.setColor(wheel);
        g.fillRect(3, 11, 2, 3);
        g.fillRect(11, 11, 2, 3);

        g.dispose();

        File out = new File("src/main/resources/assets/miningtools/textures/item/track_layer_cart.png");
        out.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", out);
    }
}
