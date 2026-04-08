/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 녹아내린 불꽃 (MoltenFlame) — 레바테인 전용 Hero 버프
 *
 * 강력한 일격으로 적의 열기 부착을 흡수하여 스택 획득.
 * 스택은 0~4이며, 4스택 시 추가 흡수 불가.
 *
 * [획득 경로]
 *   레바테인 패시브:   강력한 일격 적중 시 대상 열기 부착 전량 소모 → 소모 스택 비례 스택 획득
 *   레바테인 배틀스킬: 4스택 미만 → +1스택 / 4스택 → 전량 소모 → 대량 열기 피해 + 강제 연소 + 궁극기 충전
 *
 * [소모]
 *   레바테인 배틀스킬: 4스택 시 전량 소모
 *
 * tryAbsorbHeat()는 레바테인이 메인일 때 Hero.onFinishingBlowLanded()에서 호출됨.
 * 해당 Hero 버프가 없으면 즉시 반환하므로 다른 오퍼레이터에게 영향 없음.
 *
 * ※ 라스트 라이트와 무관. 라스트 라이트는 이 시스템을 사용하지 않음.
 */
public class MoltenFlame extends Buff {

    { type = buffType.POSITIVE; announced = true; }

    public static final int MAX_STACKS = 4;

    private int stacks = 0;

    // ─────────────────────────────────────────────
    // 스택 조작
    // ─────────────────────────────────────────────

    public int stacks() { return stacks; }

    /**
     * 스택 1 추가.
     * @return 추가 성공 여부 (4스택이면 false 반환)
     */
    public boolean addStack() {
        if (stacks >= MAX_STACKS) return false;
        stacks++;
        return true;
    }

    /**
     * 스택 전량 소모.
     * @return 소모된 스택 수
     */
    public int consumeAll() {
        int consumed = stacks;
        stacks = 0;
        if (consumed == 0) detach(); // 스택 없으면 제거
        return consumed;
    }

    // ─────────────────────────────────────────────
    // 라스트 라이트 패시브 훅
    // ─────────────────────────────────────────────

    /**
     * 강력한 일격 적중 시 호출.
     * 적이 열기 부착(HEAT)을 보유 중이면 흡수하여 +1스택.
     * Hero에 MoltenFlame 버프가 없으면 즉시 반환 (다른 오퍼레이터 영향 없음).
     */
    public static void tryAbsorbHeat(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;
        MoltenFlame flame = hero.buff(MoltenFlame.class);
        if (flame == null) return;
        if (flame.stacks >= MAX_STACKS) return; // 4스택 시 흡수 불가

        ArtsAttachment arts = target.buff(ArtsAttachment.class);
        if (arts == null || arts.currentType() != ArtsAttachment.ArtsType.HEAT) return;

        // 열기 부착 흡수 (detach) → +1스택
        arts.detach();
        flame.addStack();
    }

    // ─────────────────────────────────────────────
    // 버프 유지 (영구, 스택이 0이면 자동 제거)
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        spend(TICK);
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 녹아내린 불꽃 전용 아이콘
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString(stacks);
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String STACKS = "stacks";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(STACKS, stacks);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        stacks = bundle.getInt(STACKS);
    }
}
