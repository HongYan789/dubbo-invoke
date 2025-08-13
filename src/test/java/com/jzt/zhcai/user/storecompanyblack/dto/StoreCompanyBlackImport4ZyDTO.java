package com.jzt.zhcai.user.storecompanyblack.dto;

import java.util.List;

public class StoreCompanyBlackImport4ZyDTO {
    private Long storeId;
    private String createUserName;
    private Long createUser;
    private List<Row> rows;

    public static class Row {
        private String companyId;
        private String danwBh;
        private String freezeCause;
        private String errorMessage;
        private Boolean checkPass;

        // Getters and setters
        public String getCompanyId() { return companyId; }
        public void setCompanyId(String companyId) { this.companyId = companyId; }
        
        public String getDanwBh() { return danwBh; }
        public void setDanwBh(String danwBh) { this.danwBh = danwBh; }
        
        public String getFreezeCause() { return freezeCause; }
        public void setFreezeCause(String freezeCause) { this.freezeCause = freezeCause; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Boolean getCheckPass() { return checkPass; }
        public void setCheckPass(Boolean checkPass) { this.checkPass = checkPass; }
    }

    // Getters and setters
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    
    public String getCreateUserName() { return createUserName; }
    public void setCreateUserName(String createUserName) { this.createUserName = createUserName; }
    
    public Long getCreateUser() { return createUser; }
    public void setCreateUser(Long createUser) { this.createUser = createUser; }
    
    public List<Row> getRows() { return rows; }
    public void setRows(List<Row> rows) { this.rows = rows; }
}