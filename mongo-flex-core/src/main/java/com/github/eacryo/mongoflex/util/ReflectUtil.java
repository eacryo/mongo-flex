package com.github.eacryo.mongoflex.util;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Reflection utilities for resolving field names from lambda method references. / 反射工具类，用于从 lambda 方法引用中解析字段名。
 * <p>
 * Field name resolution follows the same JavaBeans convention as MyBatis's {@code PropertyNamer}: / 字段名解析遵循与 MyBatis {@code PropertyNamer} 相同的 JavaBeans 规范：
 * <ul>
 *   <li>{@code getName()} → {@code "name"}</li>
 *   <li>{@code isActive()} → {@code "active"}</li>
 *   <li>{@code getURL()} → {@code "URL"} (acronyms preserve case / 首字母缩写保留大写)</li>
 * </ul>
 * <p>
 * <b>Known limitation / 已知限制：</b>
 * A getter like {@code isActive()} is ambiguous — it could be the getter for a field named
 * {@code active} or a field named {@code isActive}. This method always assumes the former
 * (JavaBeans convention). If your field is literally named {@code isActive}, the resolved
 * name will be {@code "active"}, which is wrong. In that case, use {@code @CollectionField("isActive")}
 * on the field to override the mapping. / 像 {@code isActive()} 这样的 getter 存在歧义——它可能是 {@code active} 字段的 getter，也可能是 {@code isActive} 字段的 getter。本方法始终按 JavaBeans 约定采用前者。如果你的字段确实叫 {@code isActive}，解析结果会是 {@code "active"}（错误），此时请在字段上加 {@code @CollectionField("isActive")} 来覆盖映射。
 *
 * @see com.github.eacryo.mongoflex.annotation.CollectionField
 */
public class ReflectUtil {

    /**
     * ClassValue-cached writeReplace Method, one reflection per lambda class. / 使用 ClassValue 缓存 writeReplace Method，一个 lambda class 只反射一次。
     * ClassValue auto-cleans when the Class is unloaded, no memory leak risk. / ClassValue 随 Class 卸载自动清理，无内存泄漏风险。
     */
    private static final ClassValue<Method> WRITE_REPLACE_CACHE = new ClassValue<Method>() {
        @Override
        protected Method computeValue(Class<?> type) {
            try {
                Method method = type.getDeclaredMethod("writeReplace");
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Failed to resolve writeReplace on lambda class: " + type.getName(), e);
            }
        }
    };

    /**
     * Resolve the Java field name from a lambda method reference, following the MyBatis
     * {@code PropertyNamer} convention. / 从 lambda 方法引用解析 Java 字段名，遵循 MyBatis {@code PropertyNamer} 规范。
     *
     * @param func the lambda method reference, e.g. {@code Entity::getName} / lambda 方法引用，如 {@code Entity::getName}
     * @return the resolved field name / 解析出的字段名
     * @throws RuntimeException if the lambda cannot be deserialized / 如果 lambda 无法反序列化
     */
    public static <T,R> String getFieldNameFromLambda(SFunction<T,R> func) {
        try {
            SerializedLambda serializedLambda = getSerializedLambda(func);
            String methodName = serializedLambda.getImplMethodName();
            return methodToProperty(methodName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve field name from lambda", e);
        }
    }

    /**
     * Convert a getter method name to a property name, following the same rules as
     * MyBatis {@code PropertyNamer.methodToProperty()}. / 将 getter 方法名转换为属性名，规则与 MyBatis {@code PropertyNamer.methodToProperty()} 一致。
     * <p>
     * Rules / 规则：
     * <ul>
     *   <li>Strip "get" prefix (min 4 chars total) → e.g. {@code getName} → {@code Name} / 去除 "get" 前缀（至少 4 个字符）→ 如 {@code getName} → {@code Name}</li>
     *   <li>Strip "is" prefix (min 3 chars total) → e.g. {@code isActive} → {@code Active} / 去除 "is" 前缀（至少 3 个字符）→ 如 {@code isActive} → {@code Active}</li>
     *   <li>Lowercase first char, unless the second char is also uppercase (acronym) / 首字母小写，除非第二个字母也是大写（首字母缩写保留）</li>
     *   <li>If the method name does not start with "get"/"is", return it as-is (non-standard getter fallback) / 如果方法名不以 "get"/"is" 开头，原样返回（非标准 getter 兜底）</li>
     * </ul>
     * <p>
     * <b>Note / 注意：</b> The "is" prefix is ALWAYS stripped regardless of return type,
     * matching MyBatis behavior. For the ambiguous case where a boolean field is literally
     * named {@code isXxx}, use {@code @CollectionField} on the field. / "is" 前缀总是被去除（不检查返回类型），与 MyBatis 行为一致。
     * 如果 boolean 字段确实叫 {@code isXxx}，请在字段上使用 {@code @CollectionField} 注解。
     *
     * @param methodName the getter method name / getter 方法名
     * @return the resolved property name / 解析出的属性名
     */
    static String methodToProperty(String methodName) {
        String name;
        if (methodName.startsWith("get") && methodName.length() > 3) {
            name = methodName.substring(3);
        } else if (methodName.startsWith("is") && methodName.length() > 2) {
            name = methodName.substring(2);
        } else {
            // Non-standard getter name (e.g. fluent accessor like "name()") —
            // use the method name as-is. / 非标准 getter 名称（如流畅访问器 "name()"）——直接使用方法名。
            return methodName;
        }
        // JavaBeans convention: if the second char is uppercase, the first char is part
        // of an acronym and should not be lowercased (e.g. getURL → URL).
        // JavaBeans 规范：如果第二个字母大写，说明首字母是缩写的一部分，不应小写（如 getURL → URL）。
        if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
            name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
        }
        return name;
    }

    /**
     * Extract the class that actually declares the method from a lambda method reference. / 从 lambda 方法引用中提取实际声明该方法的类。
     * For example {@code LiyueCharacter::getIsAdeptus} returns {@code LiyueCharacter.class},
     * used to look up the correct {@code ClassFieldMetaData} for field name mapping. / 例如 {@code LiyueCharacter::getIsAdeptus} 返回 {@code LiyueCharacter.class}，用于字段名映射时使用正确的 {@code ClassFieldMetaData}。
     */
    public static <T, R> Class<?> getImplClassFromLambda(SFunction<T, R> func) {
        try {
            SerializedLambda sl = getSerializedLambda(func);
            String implClass = sl.getImplClass().replace('/', '.');
            return Class.forName(implClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve impl class from lambda", e);
        }
    }

    private static <T, R> SerializedLambda getSerializedLambda(SFunction<T, R> func) throws Exception {
        Method writeReplace = WRITE_REPLACE_CACHE.get(func.getClass());
        return (SerializedLambda) writeReplace.invoke(func);
    }

}

