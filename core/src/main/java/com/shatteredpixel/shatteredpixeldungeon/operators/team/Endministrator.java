/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 관리자 (Endministrator)
 *
 * 직군: 가드
 * 무기: 한손검
 * 속성: 물리
 *
 * [배틀스킬] 강타
 * [연계기]
 *   - 조건: 아군 연계기 적중 시
 *   - 효과: 물리 피해 + 오리지늄 아츠 결정 부여
 * [궁극기] 대량 물리 피해. 오리지늄 아츠 결정 소모 시 추가 피해
 * [특수] 오리지늄 아츠 결정 — 적이 물리 이상을 받을 때 소모 → 물리 피해
 *
 * ※ 가드임에도 방어불능 스택 직접 축적 불가.
 *   팀 운용 시 물리 이상 전반에 반응.
 * ※ 기본 해금 오퍼레이터 (첫 게임 시작 시 보유)
 */
public class Endministrator extends TeamOperator {

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override
    public String name() { return "관리자"; }

    @Override
    public OperatorClass operatorClass() { return OperatorClass.GUARD; }

    @Override
    public WeaponType weaponType() { return WeaponType.ONE_HANDED_SWORD; }

    @Override
    public Attribute attribute() { return Attribute.PHYSICAL; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 강타
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override
            public int baseCooldown() {
                return 4; // TODO: 수치 확정
            }

            @Override
            protected void activate(Hero hero, Char target) {
                if (target == null || !target.isAlive()) return;

                // TODO: 강타 (HEAVY_ATTACK) 물리 피해 처리 (Phase 3)
                // DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.HEAVY_ATTACK, hero);
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기 (TeamOperator 구현)
    // ─────────────────────────────────────────────

    @Override
    public int baseCooldown() {
        return 3; // TODO: 수치 확정
    }

    /**
     * 연계기 조건: 아군 연계기 적중 시
     * — 다른 TeamOperator의 activateChain() 완료 후 checkChainTriggers() 에서 평가됨
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        // TODO: 아군 연계기 적중 이벤트 플래그 확인 (Phase 3)
        // 현재는 항상 false — 실제 트리거 연결 후 구현
        return false;
    }

    /**
     * 연계기 효과: 물리 피해 + 오리지늄 아츠 결정 부여
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        // TODO: 물리 피해 처리 (Phase 3)
        // TODO: 오리지늄 아츠 결정 버프 부여 (Phase 3)

        resetCooldown();
    }

    // ─────────────────────────────────────────────
    // 궁극기: 대량 물리 피해 + 오리지늄 아츠 결정 소모 추가 피해
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override
            public int maxCharge() {
                return 100; // TODO: 수치 확정
            }

            @Override
            protected void activate(Hero hero, Char target) {
                if (target == null || !target.isAlive()) return;

                // TODO: 대량 물리 피해 처리 (Phase 3)
                // TODO: 오리지늄 아츠 결정 보유 시 추가 피해 (Phase 3)
            }
        };
    }
}
