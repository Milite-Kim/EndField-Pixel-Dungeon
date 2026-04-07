/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frozen;
import com.watabou.utils.Bundle;

/**
 * 방어불능 스택 (DefenselessStack)
 *
 * 물리 이상 4종(띄우기/넘어뜨리기/강타/갑옷파괴)이 공유하는 스택 카운터.
 *
 * [스택 없을 때]  물리 이상 적용 → 방어불능 +1스택만
 * [스택 있을 때]  물리 이상 적용 → 타입별 추가 효과 발동
 *
 * 사용법:
 *   DefenselessStack.apply(enemy, PhysicalAbnormality.LAUNCH, attacker);
 */
public class DefenselessStack extends Buff {

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    // ─────────────────────────────────────────────
    // 물리 이상 4종
    // ─────────────────────────────────────────────

    public enum PhysicalAbnormality {
        LAUNCH,        // 띄우기    - 스택 있을 때: +1스택 + 고정 물리 피해 (넘어뜨리기보다 높음)
        KNOCKDOWN,     // 넘어뜨리기 - 스택 있을 때: +1스택 + 고정 물리 피해 / 4스택 도달 시 넉백 1칸
        HEAVY_ATTACK,  // 강타      - 스택 있을 때: 전량 소모 + 소모 스택 비례 강력한 물리 피해
        ARMOR_BREAK    // 갑옷파괴   - 스택 있을 때: 전량 소모 + 소모 스택 비례 약한 물리 피해 + 갑옷파괴 디버프
    }

    public static final int MAX_STACKS = 4;

    private int stacks = 0;

    // ─────────────────────────────────────────────
    // 핵심 적용 메서드
    // ─────────────────────────────────────────────

    /**
     * 물리 이상을 적에게 적용한다.
     *
     * @param enemy    대상 적
     * @param type     물리 이상 종류
     * @param attacker 물리 이상을 유발한 공격자 (피해 계산 기준)
     */
    public static void apply(Char enemy, PhysicalAbnormality type, Char attacker) {
        // 쇄빙: 동결 상태의 적에게 물리 이상 적용 시 동결 소모 + 대량 물리 피해
        Frozen frozen = enemy.buff(Frozen.class);
        if (frozen != null && frozen.isFrozen()) {
            frozen.detach();
            triggerShattering(enemy, attacker);
        }

        DefenselessStack buff = enemy.buff(DefenselessStack.class);

        if (buff == null) {
            // 스택 없음 → 새로 생성, 1스택 부여
            buff = Buff.affect(enemy, DefenselessStack.class);
            buff.stacks = 1;

        } else {
            // 스택 있음 → 타입별 추가 효과
            switch (type) {

                case LAUNCH:
                case KNOCKDOWN:
                    // +1스택 (최대 4) + 고정 물리 피해
                    if (buff.stacks < MAX_STACKS) buff.stacks++;
                    buff.triggerLaunchOrKnockdown(enemy, type, attacker);
                    break;

                case HEAVY_ATTACK:
                    // 전량 소모 + 스택 비례 강타 피해
                    int heavyConsumed = buff.stacks;
                    buff.detach();
                    triggerHeavyAttack(enemy, heavyConsumed, attacker);
                    break;

                case ARMOR_BREAK:
                    // 전량 소모 + 스택 비례 약한 피해 + 갑옷파괴 디버프
                    int armorConsumed = buff.stacks;
                    buff.detach();
                    triggerArmorBreak(enemy, armorConsumed, attacker);
                    break;
            }
        }

        // 상태 변화 후 팀 오퍼레이터 연계기 조건 체크
        // (누가 유발했든 무관하게 항상 체크 — 교란 등 미래 기능 포함)
        if (Dungeon.hero != null && enemy.isAlive()) {
            Dungeon.hero.checkChainTriggers(enemy);
        }
    }

    // ─────────────────────────────────────────────
    // 피해 배율 상수
    // TODO: 밸런스 확정 시 아래 값을 조정
    // ─────────────────────────────────────────────

    /** 띄우기 발동 피해 배율 (공격자 damageRoll 기준). 넘어뜨리기보다 높음. TODO: 수치 확정 */
    private static final float LAUNCH_DMG_MULT    = 1.0f;

    /** 넘어뜨리기 발동 피해 배율. TODO: 수치 확정 */
    private static final float KNOCKDOWN_DMG_MULT = 0.7f;

    /**
     * 강타 발동 피해 배율 (소모 스택 1개당 damageRoll 기준).
     * 스택이 많을수록 강해지는 주력 딜링기. TODO: 수치 확정
     */
    private static final float HEAVY_ATTACK_DMG_MULT = 1.5f;

    /**
     * 갑옷파괴 발동 피해 배율 (소모 스택 1개당 damageRoll 기준).
     * 강타보다 낮은 직접 피해 + 갑옷파괴 디버프가 진짜 역할. TODO: 수치 확정
     */
    private static final float ARMOR_BREAK_DMG_MULT  = 0.5f;

    // ─────────────────────────────────────────────
    // 타입별 발동 효과
    // ─────────────────────────────────────────────

    /**
     * 띄우기/넘어뜨리기 발동 피해.
     * 스택 수와 무관하게 공격자의 damageRoll() 기반 고정 피해.
     * 넘어뜨리기로 4스택에 도달했을 때 넉백 처리.
     */
    private void triggerLaunchOrKnockdown(Char enemy, PhysicalAbnormality type, Char attacker) {
        float mult   = (type == PhysicalAbnormality.LAUNCH) ? LAUNCH_DMG_MULT : KNOCKDOWN_DMG_MULT;
        int   damage = Math.round(attacker.damageRoll() * mult);
        enemy.damage(damage, attacker, DamageType.PHYSICAL);

        // 넘어뜨리기 + 4스택 도달 시 넉백 1칸
        if (type == PhysicalAbnormality.KNOCKDOWN && stacks >= MAX_STACKS) {
            knockback(enemy, attacker);
        }
    }

    /**
     * 강타 발동 피해.
     * 소모된 스택 수 × 공격자 damageRoll() × 배율. 주력 딜링 기믹.
     */
    private static void triggerHeavyAttack(Char enemy, int consumedStacks, Char attacker) {
        int damage = Math.round(attacker.damageRoll() * consumedStacks * HEAVY_ATTACK_DMG_MULT);
        enemy.damage(damage, attacker, DamageType.PHYSICAL);
    }

    /**
     * 갑옷파괴 발동 피해 + 갑옷파괴 디버프.
     * 직접 피해는 강타보다 낮고, 갑옷파괴 디버프로 이후 피해를 증가시키는 것이 목적.
     */
    private static void triggerArmorBreak(Char enemy, int consumedStacks, Char attacker) {
        int damage = Math.round(attacker.damageRoll() * consumedStacks * ARMOR_BREAK_DMG_MULT);
        enemy.damage(damage, attacker, DamageType.PHYSICAL);
        ArmorBreaked.apply(enemy, consumedStacks);
    }

    /**
     * 쇄빙 (Shattering): 동결 상태의 적에게 물리 이상을 가했을 때 발동.
     * 동결을 소모하고 대량의 물리 피해를 준다.
     * 이후 normal DefenselessStack 처리는 계속 진행된다.
     *
     * TODO: 수치 확정 — 현재 임시 배율 3.0f 사용
     */
    private static final float SHATTERING_DMG_MULT = 3.0f; // TODO: 수치 확정

    private static void triggerShattering(Char enemy, Char attacker) {
        int damage = Math.round(attacker.damageRoll() * SHATTERING_DMG_MULT);
        enemy.damage(damage, attacker, DamageType.PHYSICAL);
    }

    /**
     * 넉백: 공격자→적 방향으로 적을 1칸 밀어낸다.
     * 목표 셀이 이동 불가하거나 다른 캐릭터가 있으면 넉백하지 않는다.
     *
     * 외부에서도 사용 가능 (예: 관통이동 등).
     */
    public static void knockback(Char enemy, Char attacker) {
        int width = Dungeon.level.width();
        int dx = Integer.signum((enemy.pos % width) - (attacker.pos % width));
        int dy = Integer.signum((enemy.pos / width) - (attacker.pos / width));

        if (dx == 0 && dy == 0) return;

        int targetCell = enemy.pos + dy * width + dx;
        if (targetCell < 0 || targetCell >= Dungeon.level.length()) return;
        if (!Dungeon.level.passable[targetCell]) return;
        if (Actor.findChar(targetCell) != null) return;

        enemy.sprite.move(enemy.pos, targetCell);
        enemy.move(targetCell, false);
    }

    // ─────────────────────────────────────────────
    // 스택 수 조회
    // ─────────────────────────────────────────────

    public int stacks() {
        return stacks;
    }

    // ─────────────────────────────────────────────
    // 버프 유지 (턴마다 자동 소모 없음 — 소모형)
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        spend(TICK);
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 전용 아이콘 추가 시 교체
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
