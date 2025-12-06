public class Medicine {
    private Long id;
    private String name;
    private String purpose;
    private String dosage;
    private String notes;
    private String recommendedTo;
    private Long userId;

    public Medicine() {}

    public Medicine(Long id, String name, String purpose, String dosage, String notes, String recommendedTo, Long userId) {
        this.id = id;
        this.name = name;
        this.purpose = purpose;
        this.dosage = dosage;
        this.notes = notes;
        this.recommendedTo = recommendedTo;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getRecommendedTo() { return recommendedTo; }
    public void setRecommendedTo(String recommendedTo) { this.recommendedTo = recommendedTo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
