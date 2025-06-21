package com.na.common.base;

import com.na.common.result.enums.NaStatus;


public class NaBaseController {
    /**
     * check params
     *
     * @param pageNo page number
     * @param pageSize page size
     * @return check result code
     */
    public NaStatus checkPageParams(int pageNo, int pageSize) {
        String msg = NaStatus.SUCCESS.getMsg();
        if (pageNo <= 0) {
            return NaStatus.PARAM_PAGE_NO_NOT_VALID;
        }
        if (pageSize <= 0) {
            return NaStatus.PARAM_PAGE_SIZE_NOT_VALID;
        }
        return NaStatus.SUCCESS;
    }
}
