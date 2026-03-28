package dss.utils;

import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class RoleUtils {
    private static final Map<String, String> ROLE_TRANSLATIONS = Map.of(
            "ROLE_ADMIN", "管理员",
            "ROLE_USER", "用户",
            "ANALYST", "分析员",
            "ECOLOGIST", "生态专家",
            "ECONOMIST", "经济专家",
            "LAWYER", "律师",
            "POWER_ENGINEER", "能源工程师"
    );

    public static String getLocalizedName(String roleName) {
        return ROLE_TRANSLATIONS.getOrDefault(roleName, roleName);
    }
}
