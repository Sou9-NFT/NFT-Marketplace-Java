package org.esprit.models;

import java.time.LocalDateTime;

public class TradeDispute {
    private int id;
    private int reporter;
    private int tradeId;
    private String offeredItem;
    private String receivedItem;
    private String reason;
    private String evidence;
    private LocalDateTime timestamp;
    private String status;
    
    // Additional fields for displaying names and titles
    private String reporterName;
    private String offeredItemTitle;
    private String receivedItemTitle;

    // Database table name constant
    public static final String TABLE_NAME = "trade_dispute";

    public TradeDispute() {
        this.timestamp = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getReporter() {
        return reporter;
    }

    public void setReporter(int reporter) {
        if (reporter <= 0) {
            throw new IllegalArgumentException("Reporter ID must be a positive number");
        }
        this.reporter = reporter;
    }

    public int getTradeId() {
        return tradeId;
    }

    public void setTradeId(int tradeId) {
        if (tradeId <= 0) {
            throw new IllegalArgumentException("Trade ID must be a positive number");
        }
        this.tradeId = tradeId;
    }

    public String getOfferedItem() {
        return offeredItem;
    }

    public void setOfferedItem(String offeredItem) {
        if (offeredItem == null || offeredItem.trim().isEmpty()) {
            throw new IllegalArgumentException("Offered item cannot be null or empty");
        }
        this.offeredItem = offeredItem;
    }

    public String getReceivedItem() {
        return receivedItem;
    }

    public void setReceivedItem(String receivedItem) {
        this.receivedItem = receivedItem;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be null or empty");
        }
        this.reason = reason;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getReporterName() {
        return reporterName;
    }
    
    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }
    
    public String getOfferedItemTitle() {
        return offeredItemTitle;
    }
    
    public void setOfferedItemTitle(String offeredItemTitle) {
        this.offeredItemTitle = offeredItemTitle;
    }
    
    public String getReceivedItemTitle() {
        return receivedItemTitle;
    }
    
    public void setReceivedItemTitle(String receivedItemTitle) {
        this.receivedItemTitle = receivedItemTitle;
    }

    /**
     * Validates all properties of the trade dispute object
     * @throws IllegalArgumentException if any validation fails
     */
    public void validate() {
        if (reporter <= 0) {
            throw new IllegalArgumentException("Reporter ID must be a positive number");
        }
        if (tradeId <= 0) {
            throw new IllegalArgumentException("Trade ID must be a positive number");
        }
        if (offeredItem == null || offeredItem.trim().isEmpty()) {
            throw new IllegalArgumentException("Offered item cannot be null or empty");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be null or empty");
        }
        if (evidence == null || evidence.trim().isEmpty()) {
            throw new IllegalArgumentException("Evidence cannot be null or empty");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
    }

    /**
     * Gets the database table name for this entity
     * @return The name of the database table
     */
    public static String getTableName() {
        return TABLE_NAME;
    }
} 