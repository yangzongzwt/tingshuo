package com.tingshuo.job.utils;

import com.tingshuo.common.core.exception.job.TaskException;
import com.tingshuo.job.constants.ScheduleConstants;
import com.tingshuo.job.entity.SysJob;
import org.quartz.*;

/**
 * 类文件描述:
 * 定时任务 工具类
 * @author yangz
 * @version 1.0.0
 * @date 2022年01月07日 20:43
 **/
public class JobUtils {
    /**
     * 创建定时任务工具类
     * @param scheduler
     * @param sysJob
     */
    public void createSysJob(Scheduler scheduler, SysJob sysJob) throws SchedulerException, TaskException {
        Class<? extends Job> jobClass = getQuartzJobClass(sysJob);
        Long jobId = sysJob.getJobId();//任务id
        String jobGroup = sysJob.getJobGroup();
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(getJobKey(sysJob)).build();
        // 表达式调度构建器
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(sysJob.getCronExpression());
        cronScheduleBuilder=handleCronScheduleMisfirePolicy(sysJob,cronScheduleBuilder);
        // 按新的cronExpression表达式构建一个新的trigger
        CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerKey(jobId, jobGroup))
                .withSchedule(cronScheduleBuilder).build();

        // 放入参数，运行时的方法可以获取
        jobDetail.getJobDataMap().put(ScheduleConstants.TASK_PROPERTIES, sysJob);

        // 判断是否存在
        if (scheduler.checkExists(getJobKey(sysJob)))
        {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(getJobKey(sysJob));
        }

        scheduler.scheduleJob(jobDetail, trigger);

        // 暂停任务
        if (sysJob.getStatus().equals(ScheduleConstants.Status.PAUSE.getValue()))
        {
            scheduler.pauseJob(getJobKey(sysJob));
        }

    }

    private static Class<? extends Job> getQuartzJobClass(SysJob sysJob) {
        boolean isConcurrent = "0".equals(sysJob.getConcurrent());
        return isConcurrent ? QuartzJobExecution.class : QuartzDisallowConcurrentExecution.class;
    }
    /**
     * 构建任务触发对象
     */
    public static TriggerKey getTriggerKey(Long jobId, String jobGroup)
    {
        return TriggerKey.triggerKey(ScheduleConstants.TASK_CLASS_NAME + jobId, jobGroup);
    }
    /**
     * 构建任务键对象
     */
    public  JobKey getJobKey(SysJob sysJob)
    {
        return JobKey.jobKey(ScheduleConstants.TASK_CLASS_NAME + sysJob.getJobId(), sysJob.getJobGroup());
    }
    /**
     * 设置定时任务策略
     */
    public static CronScheduleBuilder handleCronScheduleMisfirePolicy(SysJob job, CronScheduleBuilder cb) throws TaskException{
        switch (job.getMisfirePolicy())
        {
            case ScheduleConstants.MISFIRE_DEFAULT:
                return cb;
            case ScheduleConstants.MISFIRE_IGNORE_MISFIRES:
                return cb.withMisfireHandlingInstructionIgnoreMisfires();
            case ScheduleConstants.MISFIRE_FIRE_AND_PROCEED:
                return cb.withMisfireHandlingInstructionFireAndProceed();
            case ScheduleConstants.MISFIRE_DO_NOTHING:
                return cb.withMisfireHandlingInstructionDoNothing();
            default:
                throw new TaskException("The task misfire policy '" + job.getMisfirePolicy()
                        + "' cannot be used in cron schedule tasks", TaskException.Code.CONFIG_ERROR);
        }
    }
}
