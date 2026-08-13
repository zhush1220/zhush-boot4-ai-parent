package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件
 *
 * <p>上传成功后由 {@code FileService} 落库，物理文件由
 * zhush-spring-boot-starter-file 写入对应存储平台（本地/阿里云OSS/七牛云/华为云OBS）。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("infra_file")
public class InfraFile extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 原始文件名 */
    private String name;

    /** 相对存储路径（如 2026/08/12/xxx.jpg） */
    private String path;

    /** 文件访问URL */
    private String url;

    /** 文件扩展名（不含点号） */
    private String ext;

    /** MIME类型（如 image/png） */
    private String contentType;

    /** 文件大小（字节） */
    private Long size;

    /** 存储类型：local-本地 aliyun-oss-阿里云OSS qiniu-七牛云 huawei-obs-华为云OBS */
    private String storageType;

    /** 上传人类型：admin-管理端 member-会员端 */
    private String userType;

    /** 上传人ID（未登录为 NULL） */
    private Long userId;
}
