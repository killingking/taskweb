package com.sd.task.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.DynamicParameter;
import com.github.xiaoymin.knife4j.annotations.DynamicResponseParameters;
import com.sd.task.pojo.TaskList;
import com.sd.task.service.TaskListService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "任务清单模块")
@RestController
@RequestMapping("/api/task")
public class TaskListController {
    @Autowired
    private TaskListService taskListService;

    @ApiOperation("根据任务ID查询")
    @GetMapping("/taskList")
    public TaskList queryTaskList(Long id) {
        return taskListService.queryTaskListById(id);
    }

    @ApiOperation("添加任务接口")
    @ApiOperationSupport(ignoreParameters = {})
    @ApiImplicitParams({
//            @ApiImplicitParam(name="taskTotal",value="价格",dataType="int", paramType = "query"),
            @ApiImplicitParam(name = "type", value = "任务类型/默认点赞", dataType = "int"),
            @ApiImplicitParam(name = "videoId", value = "视频id", dataType = "String", required = true),
            @ApiImplicitParam(name = "taskTotal", value = "任务数量", dataType = "int", required = true),
            @ApiImplicitParam(name = "expireTime", value = "到期时间", dataType = "Date", required = true),
            @ApiImplicitParam(name = "price", value = "任务价格", dataType = "int", required = true)

    })
    @DynamicResponseParameters(properties = {
            @DynamicParameter(name = "id", value = "任务编号")
    })
    @PostMapping("/insOne")
    public Map<String, Object> insOneTask(TaskList taskList, HttpServletRequest request) {
        HashMap<String, Object> resMap = new HashMap<>();
        if (taskList == null) {
            resMap.put("status", 0);
            resMap.put("msg", "添加失败");
            resMap.put("id", "");
            return resMap;
        }
        int res = -1;
        try {
            res = taskListService.incremTaskList(taskList);
            if (res > 0) {
                resMap.put("status", 1);
                resMap.put("msg", "添加成功");
                resMap.put("id", taskList.getId());
            } else if(res == -2){
                resMap.put("status", res);
                resMap.put("msg", "任务期限有误");
                resMap.put("id", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resMap.put("status", -3);
            resMap.put("msg", "请勿重复添加");
            resMap.put("id", "");
        }
        return resMap;
    }


    @ApiOperation(value = "提交任务接口", notes = "需要通过任务申请接口获取视频ID,任务提交成功响应status=1任务提交成功,并返回任务信息对象")
    @ApiImplicitParams({@ApiImplicitParam(name = "videoId", value = "需要视频任务的Id", required = true),
            @ApiImplicitParam(name = "operId", value = "操作点赞者id", required = true),
            @ApiImplicitParam(name = "status", value = "任务完成状态/0-失败,1-成功", required = true),
            @ApiImplicitParam(name = "account", value = "提交任务的用户", required = true)})
    @PostMapping("/commit")
    public Map<String, Object> commitTask(@RequestParam(required = true) String videoId,
                                          @RequestParam(required = true) String operId,
                                          @RequestParam(required = true) Integer status,
                                          @RequestParam(required = true) String account) {
        Map<String, Object> resMap = new HashMap<String, Object>();
        if (videoId == null || operId == null || status == null) {
            resMap.put("status", 3);
            resMap.put("msg", "请求参数有误");
            return resMap;
        }
        resMap = taskListService.commitTaskSafe(videoId, operId, account, status);
        return resMap;
    }

    @ApiOperation(value = "申请任务接口", notes = "每个operId只能领取同一个任务一次,获取视频videoId,领取后有效时间是60秒," +
            "在时间限制内可以通过任务提交接口提交,如果没有提交,任务将失效,任务同一时刻只能领取小于任务配额总量" +
            "\n申请任务成功会返回任务信息,其中videoId用于提交任务")
    @DynamicResponseParameters(properties = {@DynamicParameter(name = "id", value = "任务编号"),
            @DynamicParameter(name = "status", value = "")
    })
    @ApiImplicitParams({@ApiImplicitParam(name = "account", value = "用户账号", required = true),
            @ApiImplicitParam(name = "operId", value = "操作点赞者id", required = true),
            @ApiImplicitParam(name = "type", value = "任务类型0-点赞 1-关注", required = true)})
    @GetMapping("/recive")
    public Map<String, Object> reciveTask(@RequestParam(required = true) String account,
                                          @RequestParam(required = true) String operId,
                                          @RequestParam(required = true) Integer type) {
        if (type == null || operId == null) {
            return (Map<String, Object>) new HashMap<String, Object>().put("msg", "请求参数有误");
        }
        Map<String, Object> resMap = taskListService.getTaskListSafe(account, operId, type);
        return resMap;
    }


    @ApiOperation("查询任务接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "pageNum", value = "页码"),
//            @ApiImplicitParam(name = "pageNo", value = "页数"),
            @ApiImplicitParam(name = "pageSize", value = "每页显示数量"),
            @ApiImplicitParam(name = "status", value = "任务状态")
    })
    @GetMapping("/list")
    public PageInfo<TaskList> getTaskList(Integer pageNum, Integer pageSize, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        List<TaskList> taskLists = taskListService.getTaskListIfUseStatus(status);
        PageInfo<TaskList> pageInfo = new PageInfo(taskLists);
        return pageInfo;
    }

    @ApiOperation("视频ID查询任务")
    @ApiImplicitParam(name = "videoId", value = "视频id", required = true)
    @GetMapping("/query")
    public TaskList getTaskListByVideoId(String videoId) {
        return taskListService.queryTaskListByVideoId(videoId);
    }
}
