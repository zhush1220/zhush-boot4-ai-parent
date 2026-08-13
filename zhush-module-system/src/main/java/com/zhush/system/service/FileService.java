package com.zhush.system.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.file.core.FileClient;
import com.zhush.file.core.FileUploadResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.satoken.core.LoginTypeConstants;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.satoken.core.StpMemberUtil;
import com.zhush.system.entity.InfraFile;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.InfraFileMapper;
import com.zhush.system.query.InfraFileQuery;
import com.zhush.system.vo.InfraFileVO;
import io.github.linpeilie.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 文件服务
 *
 * <p>上传：调用 {@link FileClient} 将物理文件写入当前启用的存储平台，
 * 成功后将文件元信息写入 {@code infra_file} 表；
 * 删除：先删除存储平台上的物理文件（尽力而为），再逻辑删除数据库记录。
 * 管理端可操作全部文件，会员端仅可操作本人上传的文件。
 *
 * @author zhush
 */
@Slf4j
@Service
public class FileService {

    private final InfraFileMapper fileMapper;
    private final FileClient fileClient;
    private final Converter converter;

    public FileService(InfraFileMapper fileMapper, FileClient fileClient, Converter converter) {
        this.fileMapper = fileMapper;
        this.fileClient = fileClient;
        this.converter = converter;
    }

    /**
     * 上传文件（上传成功后落库）
     *
     * @param file     上传的文件
     * @param userType 上传人类型（admin / member）
     */
    @Transactional(rollbackFor = Exception.class)
    public InfraFileVO uploadFile(MultipartFile file, String userType) {
        FileUploadResult result = fileClient.upload(file);

        InfraFile infraFile = new InfraFile();
        infraFile.setName(result.getOriginalName());
        infraFile.setPath(result.getPath());
        infraFile.setUrl(result.getUrl());
        infraFile.setExt(cn.hutool.core.io.file.FileNameUtil.extName(
                cn.hutool.core.util.StrUtil.blankToDefault(result.getOriginalName(), "")));
        infraFile.setContentType(result.getContentType());
        infraFile.setSize(result.getSize());
        infraFile.setStorageType(result.getStorageType());
        infraFile.setUserType(userType);
        infraFile.setUserId(resolveUserId(userType));
        fileMapper.insert(infraFile);
        return converter.convert(infraFile, InfraFileVO.class);
    }

    /**
     * 文件详情（会员端仅可查询本人文件）
     */
    public InfraFileVO getFile(Long id, String userType) {
        InfraFile infraFile = fileMapper.selectOneById(id);
        assertNotNull(infraFile, SystemErrorCode.FILE_NOT_FOUND, id);
        checkOwnership(infraFile, userType);
        return converter.convert(infraFile, InfraFileVO.class);
    }

    /**
     * 删除文件（物理删除 + 逻辑删除，会员端仅可删除本人文件）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long id, String userType) {
        InfraFile infraFile = fileMapper.selectOneById(id);
        assertNotNull(infraFile, SystemErrorCode.FILE_NOT_FOUND, id);
        checkOwnership(infraFile, userType);

        // 物理删除（存储平台删除失败仅记录日志，不阻断数据库记录删除）
        fileClient.delete(infraFile.getStorageType(), infraFile.getPath());
        fileMapper.deleteById(id);
    }

    /**
     * 文件分页
     *
     * <p>会员端强制限定为本人上传的文件；管理端可按文件名/存储类型/上传人类型过滤
     */
    public PageResult<InfraFileVO> getFilePage(InfraFileQuery query, String userType) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(InfraFile::getName, query.getName())
                .eq(InfraFile::getStorageType, query.getStorageType())
                .eq(InfraFile::getUserType, query.getUserType());
        if (LoginTypeConstants.MEMBER.equals(userType)) {
            queryWrapper.eq(InfraFile::getUserType, LoginTypeConstants.MEMBER)
                    .eq(InfraFile::getUserId, StpMemberUtil.getLoginIdAsLong());
        }
        queryWrapper.orderBy("id", false);
        Page<InfraFile> page = fileMapper.selectPageX(query, queryWrapper);
        List<InfraFileVO> list = converter.convert(page.getRecords(), InfraFileVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    // ================ 内部方法 ================

    /**
     * 会员端只能操作本人上传的文件；管理端可操作全部
     */
    private void checkOwnership(InfraFile infraFile, String userType) {
        if (!LoginTypeConstants.MEMBER.equals(userType)) {
            return;
        }
        assertTrue(LoginTypeConstants.MEMBER.equals(infraFile.getUserType())
                        && StpMemberUtil.getLoginIdAsLong() == infraFile.getUserId(),
                SystemErrorCode.FILE_NO_PERMISSION, infraFile.getId());
    }

    /**
     * 解析当前上传人ID
     */
    private Long resolveUserId(String userType) {
        if (LoginTypeConstants.ADMIN.equals(userType)) {
            return StpAdminUtil.getLoginIdDefaultNull();
        }
        return StpMemberUtil.getLoginIdDefaultNull();
    }

}
