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
import java.util.ArrayDeque;

/** Per-canvas pixel editor. Every placed flag/banner owns a separate texture slot. */
public class FlagArtScreen extends Screen {
    private static final int PIXELS_PER_BLOCK = 8;
    private static final int MAX_PIXEL_SIZE = 8;
    private static final int[] COLORS = {
            0xFFFFFFFF, 0xFF111111, 0xFF168A45, 0xFF0D4A2B,
            0xFF1769AA, 0xFF4FC3F7, 0xFFC62828, 0xFFF4C430,
            0xFF7B3FA1, 0xFFE87519, 0xFF8D8D8D, 0xFF4A2920
    };
    private final boolean banner;
    private final int designId;
    private final int canvasWidth;
    private final int canvasHeight;
    private final int[][] pixels;
    private int selectedColor;
    private int canvasX, canvasY, pixelSize, paletteY;
    private boolean bucketTool, backgroundMode;

    public FlagArtScreen(boolean banner, int designId) {
        super(Text.literal(banner ? "응원 걸개 그림판" : "응원 깃발 그림판"));
        this.banner = banner;
        this.designId = designId;
        this.canvasWidth = (banner ? 12 : 3) * PIXELS_PER_BLOCK;
        this.canvasHeight = (banner ? 3 : 5) * PIXELS_PER_BLOCK;
        this.pixels = new int[canvasHeight][canvasWidth];
        for (int y = 0; y < canvasHeight; y++) java.util.Arrays.fill(pixels[y], COLORS[0]);
        loadExistingDesign();
    }

    @Override
    protected void init() {
        int horizontalFit = Math.max(3, (width - 40) / canvasWidth);
        int verticalFit = Math.max(3, (height - 145) / canvasHeight);
        pixelSize = Math.max(3, Math.min(MAX_PIXEL_SIZE, Math.min(horizontalFit, verticalFit)));
        canvasX = (width - canvasWidth * pixelSize) / 2;
        canvasY = 48;
        paletteY = canvasY + canvasHeight * pixelSize + 10;

        addDrawableChild(ButtonWidget.builder(Text.literal("붓"), button -> {
            bucketTool = false; backgroundMode = false;
        }).dimensions(width / 2 - 150, paletteY + 23, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("채우기 도구"), button -> {
            bucketTool = !bucketTool; backgroundMode = false;
            button.setMessage(Text.literal(bucketTool ? "채우기: 켜짐" : "채우기: 꺼짐"));
        }).dimensions(width / 2 - 75, paletteY + 23, 105, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("배경색 지정"), button -> {
            backgroundMode = !backgroundMode; bucketTool = false;
            button.setMessage(Text.literal(backgroundMode ? "배경색 선택 중" : "배경색 지정"));
        }).dimensions(width / 2 + 35, paletteY + 23, 115, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("도안 저장"), button -> saveDesign())
                .dimensions(width / 2 - 105, height - 28, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("전체 지우기"), button -> clearCanvas())
                .dimensions(width / 2 + 5, height - 28, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        String ratio = banner ? "12×3 블록 · 96×24 도트" : "3×5 블록 · 24×40 도트";
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 17, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, ratio + " · 개별 디자인 " + (designId + 1) + "/64",
                width / 2, 31, 0xD0D0D0);
        for (int y = 0; y < canvasHeight; y++) {
            for (int x = 0; x < canvasWidth; x++) {
                int left = canvasX + x * pixelSize, top = canvasY + y * pixelSize;
                context.fill(left, top, left + pixelSize, top + pixelSize, pixels[y][x]);
                context.fill(left, top, left + pixelSize, top + 1, 0x66303030);
                context.fill(left, top, left + 1, top + pixelSize, 0x66303030);
            }
        }
        int startX = width / 2 - COLORS.length * 10;
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
        String mode = backgroundMode ? "팔레트에서 이 깃발의 배경색을 선택하세요" :
                bucketTool ? "채울 영역을 클릭하세요" : "마우스로 자유롭게 그리세요";
        context.drawCenteredTextWithShadow(textRenderer, mode, width / 2, paletteY + 47, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (paintAt(mouseX, mouseY, true)) return true;
        int startX = width / 2 - COLORS.length * 10;
        if (mouseY >= paletteY && mouseY < paletteY + 16) {
            int index = ((int) mouseX - startX) / 20;
            if (index >= 0 && index < COLORS.length) {
                selectedColor = index;
                if (backgroundMode) {
                    for (int y = 0; y < canvasHeight; y++) java.util.Arrays.fill(pixels[y], COLORS[index]);
                    backgroundMode = false;
                }
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && !bucketTool && !backgroundMode && paintAt(mouseX, mouseY, false)) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private boolean paintAt(double mouseX, double mouseY, boolean allowBucket) {
        int x = (int) mouseX - canvasX, y = (int) mouseY - canvasY;
        int renderedWidth = canvasWidth * pixelSize, renderedHeight = canvasHeight * pixelSize;
        if (x < 0 || y < 0 || x >= renderedWidth || y >= renderedHeight) return false;
        if (backgroundMode) return true;
        int px = x / pixelSize, py = y / pixelSize;
        if (bucketTool && allowBucket) floodFill(px, py, COLORS[selectedColor]);
        else if (!bucketTool) pixels[py][px] = COLORS[selectedColor];
        return true;
    }

    private void floodFill(int startX, int startY, int replacement) {
        int target = pixels[startY][startX];
        if (target == replacement) return;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(startY * canvasWidth + startX);
        while (!queue.isEmpty()) {
            int point = queue.removeFirst(), x = point % canvasWidth, y = point / canvasWidth;
            if (pixels[y][x] != target) continue;
            pixels[y][x] = replacement;
            if (x > 0 && pixels[y][x - 1] == target) queue.add(y * canvasWidth + x - 1);
            if (x + 1 < canvasWidth && pixels[y][x + 1] == target) queue.add(y * canvasWidth + x + 1);
            if (y > 0 && pixels[y - 1][x] == target) queue.add((y - 1) * canvasWidth + x);
            if (y + 1 < canvasHeight && pixels[y + 1][x] == target) queue.add((y + 1) * canvasWidth + x);
        }
    }

    private void clearCanvas() {
        for (int y = 0; y < canvasHeight; y++) java.util.Arrays.fill(pixels[y], COLORS[0]);
    }

    private Path textureDirectory() {
        return FabricLoader.getInstance().getGameDir()
                .resolve("resourcepacks/UltrasMod_Custom_Art/assets/ultrasmod/textures/block");
    }

    private String textureName() {
        return (banner ? "banner_cloth_" : "flag_cloth_")
                + String.format(java.util.Locale.ROOT, "%02d", designId);
    }

    private void loadExistingDesign() {
        Path saved = textureDirectory().resolve(textureName() + ".png");
        if (!Files.isRegularFile(saved)) return;
        try {
            BufferedImage image = ImageIO.read(saved.toFile());
            if (image == null || image.getWidth() != canvasWidth || image.getHeight() < canvasHeight) return;
            for (int y = 0; y < canvasHeight; y++)
                for (int x = 0; x < canvasWidth; x++) pixels[y][x] = image.getRGB(x, y);
        } catch (IOException ignored) { }
    }

    private void saveDesign() {
        try {
            Path textureDir = textureDirectory();
            Files.createDirectories(textureDir);
            Path packRoot = FabricLoader.getInstance().getGameDir().resolve("resourcepacks/UltrasMod_Custom_Art");
            Files.writeString(packRoot.resolve("pack.mcmeta"), """
                    {
                      "pack": {
                        "pack_format": 46,
                        "description": "Ultras Mod - Individual supporter flags"
                      }
                    }
                    """);
            BufferedImage animation = buildFlutterSheet();
            ImageIO.write(animation, "PNG", textureDir.resolve(textureName() + ".png").toFile());
            Files.writeString(textureDir.resolve(textureName() + ".png.mcmeta"), """
                    {
                      "animation": {
                        "width": %d,
                        "height": %d,
                        "frametime": 3,
                        "interpolate": true,
                        "frames": [0, 1, 2, 3]
                      }
                    }
                    """.formatted(canvasWidth, canvasHeight));
            if (client.player != null) client.player.sendMessage(Text.literal(
                    "개별 도안 저장 완료. 리소스 팩 'UltrasMod_Custom_Art'를 켜고 F3+T를 눌러 적용하세요."
            ), false);
            close();
        } catch (IOException exception) {
            if (client.player != null)
                client.player.sendMessage(Text.literal("도안 저장 실패: " + exception.getMessage()), false);
        }
    }

    private BufferedImage buildFlutterSheet() {
        BufferedImage image = new BufferedImage(canvasWidth, canvasHeight * 4, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < 4; frame++) {
            for (int x = 0; x < canvasWidth; x++) {
                int offset = (int) Math.round(Math.sin(x * 0.18 + frame * Math.PI / 2.0) * 2.25);
                for (int y = 0; y < canvasHeight; y++) {
                    int sourceY = Math.max(0, Math.min(canvasHeight - 1, y + offset));
                    image.setRGB(x, frame * canvasHeight + y, pixels[sourceY][x]);
                }
            }
        }
        return image;
    }
}
