package com.github.eacryo.mongoflex.bean;

import com.github.eacryo.mongoflex.annotation.CollectionField;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Nested object type for dot-notation nested field query tests / 用于点号嵌套字段查询测试的嵌套对象类型
 * <p>
 * Stored as a subdocument of {@link Character} under the key {@code home_region}. /
 * 作为 {@link Character} 的子文档存储，键名为 {@code home_region}。
 */
@Data
@NoArgsConstructor
@ToString
public class Region {

    /** Nation name: Mondstadt, Liyue, Inazuma... / 国家名：蒙德、璃月、稻妻…… */
    private String nation;

    /** Main city, mapped to main_city via @CollectionField / 主城，通过 @CollectionField 映射为 main_city */
    @CollectionField("main_city")
    private String mainCity;

    /** Altitude of the main city in meters / 主城海拔（米） */
    private Integer altitude;

    public Region(String nation, String mainCity, Integer altitude) {
        this.nation = nation;
        this.mainCity = mainCity;
        this.altitude = altitude;
    }
}
