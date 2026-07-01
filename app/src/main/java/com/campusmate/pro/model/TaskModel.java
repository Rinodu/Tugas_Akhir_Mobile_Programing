package com.campusmate.pro.model;

public class TaskModel {
    public String id;
    public String title;
    public String courseName;
    public String description;
    public String deadlineDate;
    public String deadlineTime;
    public String priority;
    public String status;
    public String completedAt;
    public String reminderType; // legacy field
    public String reminderDate;
    public String reminderTime;
    public String createdAt;

    public TaskModel() {}

    public boolean isDone() {
        return "Selesai".equalsIgnoreCase(status);
    }
}
