package kr.ultrasmod.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FlagArtScreen extends Screen {
    private static final int PIXELS_PER_BLOCK = 8;
    private static final int MAX_PIXEL_SIZE = 10;
    private static final int[] COLORS = {
            0xFF168A45, 0xFF0D4A2B, 0xFFFFFFFF, 0xFF111111,
            0xFF1769AA, 0xFF4FC3F7, 0xFFC62828, 0xFFF4C430,
            0xFF7B3FA1, 0xFFE87519, 0xFF8D8D8D, 0xFF000000
    };

    private final boolean banner;
    private final int canvasWidth;
    private final int canvasHeight;
    private final int[][] pixels;
    private int selectedColor = 0;
    private int canvasX;
    private int canvasY;
    private int pixelSize;
    private int paletteY;

    public FlagArtScreen(boolean banner) {
        super(Text.literal(banner ? "응원 걸개 그림판" : "응원 깃발 그림판"));
        this.banner = banner;

        // Match the actual in-world canvas: 12×3 blocks for banners and 3×5 for flags.
        this.canvasWidth = (banner ? 12 : 3) * PIXELS_PER_BLOCK;
        this.canvasHeight = (banner ? 3 : 5) * PIXELS_PER_BLOCK;
        this.pixels = new int[canvasHeight][canvasWidth];

        for (int y = 0; y < canvasHeight; y++) {
            for (int x = 0; x < canvasWidth; x++) {
                pixels[y][x] = 0xFF168A45;
            }
        }
    }

    @Override
    protected void init() {
        int horizontalFit = Math.max(3, (width - 40) / canvasWidth);
        int verticalFit = Math.max(3, (height - 185) / canvasHeight);
        pixelSize = Math.max(3, Math.min(MAX_PIXEL_SIZE, Math.min(horizontalFit, verticalFit)));

        int renderedWidth = canvasWidth * pixelSize;
        int renderedHeight = canvasHeight * pixelSize;
        canvasX = (width - renderedWidth) / 2;
        canvasY = 48;
        paletteY = canvasY + renderedHeight + 14;

        addDrawableChild(ButtonWidget.builder(Text.literal("디자인 저장"), button -> saveDesign())
                .dimensions(width / 2 - 105, height - 32, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("전체 지우기"), button -> clearCanvas())
                .dimensions(width / 2 + 5, height - 32, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        String ratio = banner ? "12:3 비율 · 96×24 도트" : "3:5 비율 · 24×40 도트";
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, ratio + " · 실제 설치 크기와 같은 비율", width / 2, 32, 0xD0D0D0);

        for (int y = 0; y < canvasHeight; y++) {
            for (int x = 0; x < canvasWidth; x++) {
                int left = canvasX + x * pixelSize;
                int top = canvasY + y * pixelSize;
                context.fill(left, top, left + pixelSize, top + pixelSize, pixels[y][x]);
                if (pixelSize >= 5) {
                    context.fill(left, top, left + pixelSize, top + 1, 0x55303030);
                    context.fill(left, top, left + 1, top + pixelSize, 0x55303030);
                }
            }
        }

        int startX = width / 2 - (COLORS.length * 20) / 2;
        for (int i = 0; i < COLORS.length; i++) {
            int x = startX + i * 20;
            context.fill(x, paletteY, x + 16, paletteY + 16, COLORS[i]);
            if (i == selectedColor) {
                context.fill(x - 2, paletteY - 2, x + 18, paletteY, 0xFFFFFF00);
                context.fill(x - 2, paletteY + 16, x + 18, paletteY + 18, 0xFFFFFF00);
                context.fill(x - 2, paletteY, x, paletteY + 16, 0xFFFFFF00);
                context.fill(x + 16, paletteY, x + 18, paletteY + 16, 0xFFFFFF00);
            }
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (paintAt(mouseX, mouseY)) return true;

        int startX = width / 2 - (COLORS.length * 20) / 2;
        if (mouseY >= paletteY && mouseY < paletteY + 16) {
            int index = ((int) mouseX - startX) / 20;
            if (index >= 0 && index < COLORS.length) selectedColor = index;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && paintAt(mouseX, mouseY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private boolean paintAt(double mouseX, double mouseY) {
        int x = (int) mouseX - canvasX;
        int y = (int) mouseY - canvasY;
        int renderedWidth = canvasWidth * pixelSize;
        int renderedHeight = canvasHeight * pixelSize;
        if (x < 0 || y < 0 || x >= renderedWidth || y >= renderedHeight) return false;

        pixels[y / pixelSize][x / pixelSize] = COLORS[selectedColor];
        return true;
    }

    private void clearCanvas() {
        for (int y = 0; y < canvasHeight; y++) {
            for (int x = 0; x < canvasWidth; x++) {
                pixels[y][x] = 0xFFFFFFFF;
            }
        }
    }

    private void saveDesign() {
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            Path packRoot = gameDir.resolve("resourcepacks/UltrasMod_Custom_Art");
            Path textureDir = packRoot.resolve("assets/ultrasmod/textures/block");
            Files.createDirectories(textureDir);

            Files.writeString(packRoot.resolve("pack.mcmeta"), """
                    {
                      "pack": {
                        "pack_format": 46,
                        "description": "Ultras Mod - Custom supporter art"
                      }
                    }
                    """);

            BufferedImage animation = buildFlutterSheet();
            String textureName = banner ? "banner_cloth" : "flag_cloth";
            ImageIO.write(animation, "PNG", textureDir.resolve(textureName + ".png").toFile());
            Files.writeString(textureDir.resolve(textureName + ".png.mcmeta"), """
                    {
                      "animation": {
                        "width": %d,
                        "height": %d,
                        "frametime": 4,
                        "interpolate": true,
                        "frames": [0, 1, 2, 3]
                      }
                    }
                    """.formatted(canvasWidth, canvasHeight));

            if (client.player != null) {
                client.player.sendMessage(Text.literal(
                        "도안 저장 완료! 설정 > 리소스 팩에서 'UltrasMod_Custom_Art'를 켠 뒤 F3+T를 눌러 적용해."
                ), false);
            }
            close();
        } catch (IOException exception) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("도안 저장 실패: " + exception.getMessage()), false);
            }
        }
    }

    private BufferedImage buildFlutterSheet() {
        BufferedImage image = new BufferedImage(canvasWidth, canvasHeight * 4, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < 4; frame++) {
            for (int x = 0; x < canvasWidth; x++) {
                int offset = (int) Math.round(Math.sin((x * 0.18) + (frame * Math.PI / 2.0)) * 1.4);
                for (int y = 0; y < canvasHeight; y++) {
                    int sourceY = Math.max(0, Math.min(canvasHeight - 1, y + offset));
                    image.setRGB(x, frame * canvasHeight + y, pixels[sourceY][x]);
                }
            }
        }
        return image;
    }
}
