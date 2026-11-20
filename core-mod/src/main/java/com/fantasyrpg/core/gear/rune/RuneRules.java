package com.fantasyrpg.core.gear.rune;

import com.fantasyrpg.core.gear.armor.ArmorPart;

import java.util.ArrayList;
import java.util.List;

/**
 * 룬석 끼우기·빼기·합성·연마 규칙. Minecraft 비의존 (claude/계열방어구_상위옵션_초안.md 3장).
 *
 * <ul>
 *   <li>합계 상한 없음: 같은 룬석을 여러 칸에 끼우면 그대로 더해진다</li>
 *   <li>갑옷 등급이 룬석 등급을 제한하지 않는다</li>
 *   <li>빼기는 마지막에 끼운 룬석부터</li>
 *   <li>합성: 같은 종류·같은 등급 3개 → 한 등급 위 1개 (최고급은 합성 불가)</li>
 *   <li>연마: 룬석 1개 + 같은 등급 연마재 → 한 등급 위 (실패 없음, 최고급은 불가)</li>
 * </ul>
 */
public final class RuneRules {
    /** 합성에 필요한 개수 */
    public static final int COMBINE_COUNT = 3;
    /** 연마재 등급 (하급·중급·상급용) */
    public static final int MAX_ABRASIVE_GRADE = RuneDefinition.MAX_GRADE - 1;

    private RuneRules() {
    }

    /** 끼운 룬석 하나 */
    public record Socketed(RuneType type, int grade) {
    }

    public enum InsertResult { OK, NO_FREE_SOCKET, WRONG_PART, LIGHT_ONLY }

    public static InsertResult canInsert(RuneDefinition def, ArmorPart part, int filled, int socketCount) {
        return canInsert(def, part, filled, socketCount, true);
    }

    /** lightArmor = 엘프·마도 갑옷 (직업 룬석은 경갑에만) */
    public static InsertResult canInsert(RuneDefinition def, ArmorPart part, int filled, int socketCount, boolean lightArmor) {
        if (!def.fits(part)) return InsertResult.WRONG_PART;
        if (def.lightOnly() && !lightArmor) return InsertResult.LIGHT_ONLY;
        if (filled >= socketCount) return InsertResult.NO_FREE_SOCKET;
        return InsertResult.OK;
    }

    /** 끼운 뒤 목록 (새 목록) */
    public static List<Socketed> insert(List<Socketed> current, Socketed rune) {
        List<Socketed> out = new ArrayList<>(current);
        out.add(rune);
        return out;
    }

    /** 마지막 룬석을 뺀 목록. 비어 있으면 그대로 */
    public static List<Socketed> removeLast(List<Socketed> current) {
        if (current.isEmpty()) return current;
        return List.copyOf(current.subList(0, current.size() - 1));
    }

    /** 합성 결과 등급. 불가능하면 0 */
    public static int combineResult(int grade, int count) {
        if (count != COMBINE_COUNT || grade < RuneDefinition.MIN_GRADE || grade >= RuneDefinition.MAX_GRADE) return 0;
        return grade + 1;
    }

    /** 연마 결과 등급. 연마재 등급이 룬석 등급과 같아야 한다. 불가능하면 0 */
    public static int polishResult(int runeGrade, int abrasiveGrade) {
        if (runeGrade < RuneDefinition.MIN_GRADE || runeGrade >= RuneDefinition.MAX_GRADE) return 0;
        if (abrasiveGrade != runeGrade) return 0;
        return runeGrade + 1;
    }

    /**
     * 갑옷 한 부위에서 실제로 효과가 나는 룬석 (소켓 수를 넘는 것과 부위가 맞지 않는 것은 제외).
     * 결과의 칸 번호는 1부터.
     */
    public static List<Active> active(List<Socketed> sockets, int socketCount, ArmorPart part,
                                      java.util.function.Function<RuneType, RuneDefinition> defs) {
        List<Active> out = new ArrayList<>();
        for (int i = 0; i < sockets.size() && i < socketCount; i++) {
            Socketed s = sockets.get(i);
            RuneDefinition def = defs.apply(s.type());
            if (def != null && def.fits(part)) out.add(new Active(i + 1, def, s.grade()));
        }
        return out;
    }

    /** 효과가 나는 룬석: 칸 번호, 정의, 등급 */
    public record Active(int slot, RuneDefinition def, int grade) {
    }
}
