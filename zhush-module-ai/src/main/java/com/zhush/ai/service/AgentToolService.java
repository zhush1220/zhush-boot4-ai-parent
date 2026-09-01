package com.zhush.ai.service;

import com.zhush.ai.vo.AgentToolVO;
import com.zhush.agentscope.tool.RegisteredTool;
import com.zhush.agentscope.tool.ToolkitRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 已注册工具服务（只读列表）。
 *
 * @author zhush
 */
@Service
public class AgentToolService {

    private final ToolkitRegistry toolkitRegistry;

    public AgentToolService(ToolkitRegistry toolkitRegistry) {
        this.toolkitRegistry = toolkitRegistry;
    }

    /**
     * 列出全部已注册工具。
     */
    public List<AgentToolVO> list() {
        List<RegisteredTool> tools = toolkitRegistry.all();
        List<AgentToolVO> result = new ArrayList<>(tools.size());
        for (RegisteredTool tool : tools) {
            AgentToolVO vo = new AgentToolVO();
            vo.setName(tool.getName());
            vo.setDescription(tool.getDescription());
            vo.setBeanName(tool.getBeanName());
            vo.setRequiresApproval(tool.isRequiresApproval());
            vo.setGroupCode(tool.getGroupCode());
            result.add(vo);
        }
        return result;
    }
}
