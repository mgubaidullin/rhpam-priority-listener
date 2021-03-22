package org.exampe;

import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.jbpm.services.api.model.UserTaskInstanceDesc;
import org.jbpm.services.api.service.ServiceRegistry;
import org.jbpm.workflow.instance.node.HumanTaskNodeInstance;
import org.kie.api.event.process.*;
import org.kie.api.runtime.query.QueryContext;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.query.QueryFilter;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class PrioritizerListener implements ProcessEventListener {

    RuntimeDataService dataService = (RuntimeDataService) ServiceRegistry.get().service(ServiceRegistry.RUNTIME_DATA_SERVICE);
    UserTaskService taskService = (UserTaskService) ServiceRegistry.get().service(ServiceRegistry.USER_TASK_SERVICE);

    String processName;
    String variableName;
    String taskName;

    public PrioritizerListener(String processName, String variableName, String taskName) {
        this.processName = processName;
        this.variableName = variableName;
        this.taskName = taskName;
    }

    @Override
    public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
        if (event.getNodeInstance() instanceof HumanTaskNodeInstance && Objects.equals(event.getNodeInstance().getNodeName(), taskName)) {
            sortTasks();
        }
    }

    private void sortTasks() {
        System.out.println("SORT TASKS");
        // get processes
        Collection<ProcessInstanceDesc> instances =
                dataService.getProcessInstancesByProcessDefinition(processName, Arrays.asList(1), new QueryContext());

        // get tasks
        List<TaskSummary> tasks = new ArrayList<>();
        instances.forEach(pid -> dataService.getTasksByStatusByProcessInstanceId(pid.getId(),
                Arrays.asList(Status.Ready, Status.Reserved, Status.Created), new QueryFilter())
                .forEach(taskSummary -> {
                    if (taskSummary.getName().equals(taskName)) {
                        tasks.add(taskSummary);
                    }
                }));

        // sort tasks by value of (variableName)
        List<UserTaskInstanceDesc> sortedTasks =  tasks.stream()
                .map(t -> dataService.getTaskById(t.getId()))
                .sorted((o1, o2) ->getVariableValue(o2.getTaskId()).compareTo(getVariableValue(o1.getTaskId())))
                .collect(Collectors.toList());

        // set task priority
        for (int i = 0; i < sortedTasks.size(); i++) {
            UserTaskInstanceDesc t = sortedTasks.get(i);
            taskService.setPriority(t.getTaskId(), i);
            System.out.println(t);
        }
    }

    private BigDecimal getVariableValue(long taskId) {
        String v = (String) taskService.getTaskInputContentByTaskId(taskId).get(variableName);
        return new BigDecimal(v);
    }

    @Override
    public void beforeProcessStarted(ProcessStartedEvent event) {

    }

    @Override
    public void afterProcessStarted(ProcessStartedEvent event) {

    }

    @Override
    public void beforeProcessCompleted(ProcessCompletedEvent event) {

    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {

    }

    @Override
    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {

    }

    @Override
    public void beforeNodeLeft(ProcessNodeLeftEvent event) {

    }

    @Override
    public void afterNodeLeft(ProcessNodeLeftEvent event) {

    }

    @Override
    public void beforeVariableChanged(ProcessVariableChangedEvent event) {

    }

    @Override
    public void afterVariableChanged(ProcessVariableChangedEvent event) {

    }
}
