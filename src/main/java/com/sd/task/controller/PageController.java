package com.sd.task.controller;

import com.sd.task.pojo.TaskList;
import com.sd.task.service.TaskListService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "页面跳转模块")
@Controller
public class PageController {

    @Resource
    private TaskListService taskListService;
//    @RequestMapping("/page/test")
//    public String goTest() {
//        return "/test";
//    }

//    @ApiOperation("操作添加任务")
//    @RequestMapping("/taskIndex")
//    public String goTaskIndex(){
//        return "taskIndex";
//    }

    @ApiOperation(value = "查询任务情况http://47.94.136.121:8010/",
    notes="任务操作前端请移步http://47.94.136.121:8010/#/form/index")
    @GetMapping("/showTask")
    public String goShowTask(ModelMap mv){
//        ModelMap mv = new ModelMap();
        List<TaskList> taskLists = taskListService.getTaskListIfUseStatus(1);
        mv.addAttribute("taskLists",taskLists);
        return "showTask";
    }
}
