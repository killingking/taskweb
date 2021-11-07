package com.sd.task.controller;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.xiaoymin.knife4j.annotations.DynamicParameter;
import com.github.xiaoymin.knife4j.annotations.DynamicResponseParameters;
import com.sd.task.pojo.TaskList;
import com.sd.task.pojo.dto.JSONResult;
import com.sd.task.service.TaskListService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@Api(tags = "任务清单模块")
@RestController
@RequestMapping("/api/task")
public class TaskListController {
    @Autowired
    private TaskListService taskListService;

    @ApiOperation("根据任务ID查询")
    @GetMapping(value = "/taskList", produces = "application/json;charset=UTF-8")
    public String queryTaskList(Long id) {
        TaskList taskList = taskListService.queryTaskListById(id);
        return JSONResult.fillResultString(200, "查询成功", JSON.toJSONString(taskList));
    }

    @ApiOperation("添加任务接口")
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
    @PostMapping(value = "/insOne", produces = "application/json;charset=UTF-8")
    public String insOneTask(@Validated TaskList taskList) {
        HashMap<String, Object> resMap = new HashMap<>();
        try {
            taskListService.incremTaskList(taskList);
            resMap.put("id", taskList.getId());
            return JSONResult.fillResultString(201, "创建成功", resMap);
        } catch (Exception e) {
            return JSONResult.fillResultString(0, e.getMessage(), null);
        }
    }


    @ApiOperation(value = "提交任务接口", notes = "需要通过任务申请接口获取视频ID,任务提交成功响应status=1任务提交成功,并返回任务信息对象")
    @ApiImplicitParams({@ApiImplicitParam(name = "videoId", value = "需要视频任务的Id", required = true),
            @ApiImplicitParam(name = "operId", value = "操作点赞者id", required = true),
            @ApiImplicitParam(name = "status", value = "任务完成状态/0-失败,1-成功", required = true),
            @ApiImplicitParam(name = "account", value = "提交任务的用户", required = true)})
    @PostMapping(value = "/commit", produces = "application/json;charset=UTF-8")
    public String commitTask(@RequestParam(required = true) String videoId,
                             @RequestParam(required = true) String operId,
                             @RequestParam(required = true) Integer status,
                             @RequestParam(required = true) String account) {
        if (videoId == null || operId == null || status == null) {
            return JSONResult.fillResultString(400, "请求参数有误", null);
        }
        try {
            TaskList task = taskListService.commitTaskSafe(videoId, operId, account, status);
            return JSONResult.fillResultString(1, "提交成功", null);
        } catch (Exception e) {
            return JSONResult.fillResultString(0, e.getMessage(), null);
        }
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
    @GetMapping(value = "/recive", produces = "application/json;charset=UTF-8")
    public String reciveTask(@RequestParam(required = true) String account,
                             @RequestParam(required = true) String operId,
                             @RequestParam(required = true) Integer type) {
        if (type == null || operId == null) {
            return JSONResult.fillResultString(400, "请求参数有误", null);
        }
        try {
            HashMap<String, Object> taskMap = new HashMap<>();
            TaskList task = taskListService.getTaskListSafe(account, operId, type);
            taskMap.put("id", task.getId());
            taskMap.put("price", task.getPrice());
            taskMap.put("uid", task.getUid());
            taskMap.put("type", task.getType());
            taskMap.put("videoId", task.getVideoId());
            return JSONResult.fillResultString(1, "申请任务成功", taskMap);
        } catch (Exception e) {
            return JSONResult.fillResultString(0, e.getMessage(), null);
        }
    }


    @ApiOperation("查询任务接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "pageNum", value = "页码"),
            @ApiImplicitParam(name = "pageSize", value = "每页显示数量"),
            @ApiImplicitParam(name = "status", value = "任务状态")
    })
    @GetMapping(value = "/list", produces = "application/json;charset=UTF-8")
    public String getTaskList(Integer pageNum, Integer pageSize, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        List<TaskList> taskLists = taskListService.getTaskListIfUseStatus(status);
        PageInfo<TaskList> pageInfo = new PageInfo(taskLists);
        return JSONResult.fillResultString(1, "success", pageInfo);
    }

    @ApiOperation("视频ID查询任务")
    @ApiImplicitParam(name = "videoId", value = "视频id", required = true)
    @GetMapping(value = "/query", produces = "application/json;charset=UTF-8")
    public String getTaskListByVideoId(String videoId) {
        TaskList task = taskListService.queryTaskListByVideoId(videoId);
        return JSONResult.fillResultString(1, "success", task);
    }
}
