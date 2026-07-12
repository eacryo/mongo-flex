package com.github.eacryo.mongoflex.constant;

/**
 * Fill strategy constants (reserved for future extension). / 填充策略常量（为将来扩展预留）。
 */
public class FillConstant {
    /** Insert-only fill / 仅插入时填充 */
    public static final String INSERT = "insert";
    /** Insert and update fill / 插入和更新时填充 */
    public static final String INSERT_UPDATE = "insert_update";
    /** Update-only fill / 仅更新时填充 */
    public static final String UPDATE = "update";
    /** Delete fill / 删除时填充 */
    public static final String DELETE = "delete";

    private FillConstant() {
    }
}
