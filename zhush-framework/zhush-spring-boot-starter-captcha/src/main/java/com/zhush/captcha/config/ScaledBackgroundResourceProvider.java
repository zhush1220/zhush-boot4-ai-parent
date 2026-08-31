package com.zhush.captcha.config;

import cloud.tianai.captcha.resource.AbstractResourceProvider;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 背景图统一缩放资源提供者
 *
 * <p>tianai-captcha 滑块验证码的拼图块尺寸由内置模板决定（固定），而背景图直接使用资源原图，
 * 若背景图分辨率过大，前端按容器宽度等比缩放（scale &lt; 1）会把拼图块缩得很小，视觉上
 * 「背景图占位大、拼图块偏小」。本提供者把背景图统一缩放为固定宽度（高度按原始比例自适应），
 * 使前端缩放比接近 1，拼图块显示尺寸协调。
 *
 * <p>资源 data 为 classpath 相对路径（如 {@code captcha/1.jpg} 或 jar 内置图片路径），
 * 与 {@link #NAME} 类型一起由 {@link CaptchaAutoConfiguration} 注册。
 *
 * @author zhush
 */
@Slf4j
public class ScaledBackgroundResourceProvider extends AbstractResourceProvider {

    /** 资源类型标识（与 ClassPathResourceProvider 的 "classpath" 并列，独占一种 provider） */
    public static final String NAME = "scaled-bg";

    /** 基准背景图缩放宽度（px），高度按原始比例自适应 */
    private static final int BASE_WIDTH = 300;

    /** 拼图块显示缩小系数（1 - 15%）：把背景图原始宽度放大到 BASE_WIDTH / 该系数，
     *  前端按固定显示宽度缩放时，拼图块与背景缺口同步缩小 15%（二者来自同一模板尺寸，始终吻合） */
    private static final double BLOCK_SHRINK_RATIO = 0.85;

    /** 实际背景图缩放宽度 = 基准宽 / 缩小系数（≈353） */
    private static final int TARGET_WIDTH =
            (int) Math.round(BASE_WIDTH / BLOCK_SHRINK_RATIO);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean supported(Resource resource) {
        return NAME.equals(resource.getType());
    }

    @Override
    public InputStream doGetResourceInputStream(Resource resource) {
        String path = stripPrefix(resource.getData());
        try (InputStream in = openStream(path)) {
            if (in == null) {
                log.warn("[验证码] 未找到背景图资源: {}", path);
                return null;
            }
            BufferedImage source = ImageIO.read(in);
            if (source == null) {
                log.warn("[验证码] 背景图无法识别（可能为 webp，不支持缩放），使用原图: {}", path);
                return reOpen(path);
            }
            int targetHeight = Math.max(1,
                    Math.round(TARGET_WIDTH * (float) source.getHeight() / source.getWidth()));
            BufferedImage scaled = new BufferedImage(TARGET_WIDTH, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, TARGET_WIDTH, targetHeight, null);
            graphics.dispose();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(scaled, "png", buffer);
            return new ByteArrayInputStream(buffer.toByteArray());
        } catch (Exception e) {
            log.warn("[验证码] 背景图缩放失败，使用原图: {}", e.getMessage());
            return reOpen(path);
        }
    }

    /** 去掉资源路径上的 "classpath:" 前缀，得到 classloader 可读取的相对路径 */
    private String stripPrefix(String data) {
        if (data == null) {
            return null;
        }
        return data.startsWith("classpath:") ? data.substring("classpath:".length()) : data;
    }

    /** 通过 classloader 读取 classpath 资源 */
    private InputStream openStream(String path) {
        if (path == null) {
            return null;
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream in = loader == null ? null : loader.getResourceAsStream(path);
        if (in == null) {
            in = getClass().getClassLoader().getResourceAsStream(path);
        }
        return in == null ? null : new BufferedInputStream(in);
    }

    /** 重新打开原图输入流（缩放失败或不适于缩放时回退原图） */
    private InputStream reOpen(String path) {
        return openStream(path);
    }
}
