package com.na.common.base;

import com.na.common.result.enums.Status;


public class BaseController {
    /**
     * check params
     *
     * @param pageNo page number
     * @param pageSize page size
     * @return check result code
     */
    public Status checkPageParams(int pageNo, int pageSize) {
        String msg = Status.SUCCESS.getMsg();
        if (pageNo <= 0) {
            return Status.PARAM_PAGE_NO_NOT_VALID;
        }
        if (pageSize <= 0) {
            return Status.PARAM_PAGE_SIZE_NOT_VALID;
        }
        return Status.SUCCESS;
    }
}
