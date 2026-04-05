/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.Operator;
import com.shatteredpixel.shatteredpixeldungeon.operators.OperatorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * 헤드헌팅 허가증 (Headhunting Permit)
 *
 * 5층 보스 처치 시 드랍된다 (파티 만원 여부 무관).
 * 사용 시 현재 파티에 없는 오퍼레이터 4명을 랜덤으로 제시하고
 * 그 중 1명을 팀 오퍼레이터로 영입할 수 있다.
 *
 * - 리롤: 허가증 1장당 1회, 크레디트 소모 (TODO: 비용 미확정)
 * - 파티 만원 시: 기존 팀 오퍼레이터와 교체 가능 (교체된 오퍼레이터 영구 삭제)
 */
public class HeadhuntingPermit extends Item {

    /** 허가증 1장당 제시되는 오퍼레이터 수 */
    public static final int CANDIDATES = 4;

    /** 허가증당 리롤 가능 횟수 */
    public static final int MAX_REROLLS = 1;

    public static final String AC_USE = "USE";

    {
        image        = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트 추가
        stackable    = true;
        defaultAction = AC_USE;
        unique       = false;
    }

    // ─────────────────────────────────────────────
    // 아이템 액션
    // ─────────────────────────────────────────────

    @Override
    public java.util.ArrayList<String> actions(Hero hero) {
        java.util.ArrayList<String> actions = super.actions(hero);
        actions.add(AC_USE);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (action.equals(AC_USE)) {
            doUse(hero);
        }
    }

    private void doUse(Hero hero) {
        List<Class<? extends Operator>> pool = OperatorRegistry.getRecruitPool(hero);

        if (pool.isEmpty()) {
            // 모든 오퍼레이터가 파티에 있음 (구현된 오퍼레이터가 적을 때 발생 가능)
            GLog.w("영입 가능한 오퍼레이터가 없습니다.");
            return;
        }

        // 후보 4명 랜덤 선발
        List<Class<? extends Operator>> candidates = pickCandidates(pool, CANDIDATES);

        // TODO: 헤드헌팅 선택 UI 호출
        //   HeadhuntingScene.show(hero, candidates, MAX_REROLLS, this);
        //   → 플레이어가 선택하면 hero.addTeamOperator(op) 또는 hero.replaceTeamOperator(old, new) 호출
        //   → 사용된 허가증 소모: detach(hero.belongings)

        // 현재는 UI 미구현이므로 로그만 출력
        StringBuilder sb = new StringBuilder("[헤드헌팅 허가증] 후보 오퍼레이터:\n");
        for (Class<? extends Operator> c : candidates) {
            sb.append("  - ").append(c.getSimpleName()).append("\n");
        }
        GLog.i(sb.toString());
        GLog.w("TODO: 헤드헌팅 UI가 아직 구현되지 않았습니다.");
    }

    /**
     * 풀에서 최대 count 명의 후보를 랜덤하게 뽑는다.
     * 풀이 count보다 작으면 전체 반환.
     */
    private static List<Class<? extends Operator>> pickCandidates(
            List<Class<? extends Operator>> pool, int count) {

        List<Class<? extends Operator>> shuffled = new ArrayList<>(pool);
        // Fisher-Yates shuffle
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = Random.Int(i + 1);
            Class<? extends Operator> tmp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, tmp);
        }
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    // ─────────────────────────────────────────────
    // 아이템 정보
    // ─────────────────────────────────────────────

    @Override
    public boolean isUpgradable() { return false; }

    @Override
    public boolean isIdentified()  { return true;  }
}
