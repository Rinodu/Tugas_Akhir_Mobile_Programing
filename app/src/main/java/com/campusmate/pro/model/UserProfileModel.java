package com.campusmate.pro.model;

public class UserProfileModel {
    public String studentName;
    public String university;
    public String major;
    public int semester;
    public String studyGoal;
    public int dailyStudyHours;

    public UserProfileModel() {}

    public UserProfileModel(String studentName, String university, String major, int semester, String studyGoal, int dailyStudyHours) {
        this.studentName = studentName;
        this.university = university;
        this.major = major;
        this.semester = semester;
        this.studyGoal = studyGoal;
        this.dailyStudyHours = dailyStudyHours;
    }
}
