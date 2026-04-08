/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DefenselessStack;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WolfClaw;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;

/**
 * 로시 (Rosi)
 *
 * 직군: 가드
 * 무기: 한손검
 * 속성: 물리+열기 크로스
 *
 * [배틀스킬] 물리 피해(×SKILL_MULT) + 띄우기(LAUNCH)
 *             방어불능 스택 보유 적 적중 시 → 추가 열기 피해(×SKILL_HEAT_MULT) + 늑대의 발톱 부여
 *
 * [연계기]   조건: 방어불능 스택 + 아츠 부착 동시 보유 시
 *             효과: 물리 피해(×CHAIN_MULT) + 모든 아츠 부착 소모(침묵 소모) → 특수 띄우기(방어불능 2스택 추가)
 *             ※ 아츠 부착→방어불능 변환 브릿지 역할
 *
 * [궁극기]   대량 열기 피해(×ULT_MULT) + 열기 부착
 *             늑대의 발톱 보유 시 → 추가 열기 피해(×ULT_WOLF_BONUS_MULT) + Hero 회복(HT×ULT_RECOVERY_RATIO)
 *
 * [특수] 늑대의 발톱(WolfClaw):
 *   - 매 턴 물리 틱 피해(×WolfClaw.TICK_DMG_MULT)
 *   - 보유 중 물리/열기 피해 ×WolfClaw.DMG_AMP_MULT 증폭
 */
public class Rosi extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해 배율 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 기본 물리 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT           = 1.2f;

    /** 배틀스킬 조건부 추가 열기 피해 배율 (방어불능 스택 보유 시). TODO: 수치 확정 */
    private static final float SKILL_HEAT_MULT      = 0.8f;

    /** 연계기 물리 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT           = 1.0f;

    /** 궁극기 열기 피해 배율. TODO: 수치 확정 */
    private static final float ULT_MULT             = 2.0f;

    /** 궁극기 늑대의 발톱 조건부 추가 열기 피해 배율. TODO: 수치 확정 */
    private static final float ULT_WOLF_BONUS_MULT  = 1.0f;

    /** 궁극기 늑대의 발톱 조건부 회복 비율 (Hero.HT 기준). TODO: 수치 확정 */
    private static final float ULT_RECOVERY_RATIO   = 0.15f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()           { return "로시"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.GUARD; }
    @Override public WeaponType weaponType() { return WeaponType.ONE_HANDED_SWORD; }
    @Override public Attribute attribute()   { return Attribute.PHYSICAL; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 물리 피해 + 띄우기 (+방어불능 보유 시 열기 피해 + 늑대의 발톱)
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override
            public int baseCooldown() { return 4; } // TODO: 수치 확정

            @Override
            public String name() { return "늑대 발톱 베기"; } // TODO: 정식 스킬명 확정

            @Override
            public String description() {
                return "물리 피해(" + SKILL_MULT + "×) + 띄우기.\n" +
                       "방어불능 스택 보유 적: 추가 열기 피해(" + SKILL_HEAT_MULT + "×) + 늑대의 발톱 부여.\n" +
                       "TODO: 수치 확정";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 방어불능 스택 보유 여부를 피해 이전에 확인 (LAUNCH 이후 변경될 수 있음)
                boolean hadStacks = target.buff(DefenselessStack.class) != null;

                // 물리 피해
                int damage = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(damage, hero, DamageType.PHYSICAL);

                // 띄우기 (스택 추가 또는 이상 발동)
                if (target.isAlive()) {
                    DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.LAUNCH, hero);
                }

                // 조건부: 방어불능 스택 보유 시 → 추가 열기 피해 + 늑대의 발톱
                if (hadStacks && target.isAlive()) {
                    int heatDmg = Math.round(hero.damageRoll() * SKILL_HEAT_MULT);
                    target.damage(heatDmg, hero, DamageType.HEAT);

                    if (target.isAlive()) {
                        WolfClaw.apply(target, WolfClaw.DEFAULT_DURATION);
                    }
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 물리 피해 + 아츠 부착 전량 소모 → 특수 띄우기(방어불능 2스택)
    // ─────────────────────────────────────────────

    @Override
    public int baseCooldown() { return 3; } // TODO: 수치 확정

    @Override
    public String chainName() { return "야수 강탈"; } // TODO: 정식 스킬명 확정

    @Override
    public String chainDescription() {
        return "조건: 방어불능 스택 + 아츠 부착 동시 보유 시\n" +
               "물리 피해(" + CHAIN_MULT + "×) + 모든 아츠 부착 소모(침묵) → 방어불능 2스택 추가.\n" +
               "※ 아츠→방어불능 변환 브릿지";
    }

    /**
     * 연계기 조건: 방어불능 스택 AND 아츠 부착 동시 보유.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        return target.buff(DefenselessStack.class) != null
                && target.buff(ArtsAttachment.class) != null;
    }

    /**
     * 연계기 효과:
     * 1. 물리 피해
     * 2. 아츠 부착 전량 소모 (폭발/반응 없이 침묵 소모 — detach만 호출)
     * 3. 특수 띄우기: 방어불능 스택 2개 추가 (이상 발동 없음)
     *
     * "특수 띄우기"의 의미: 일반 LAUNCH처럼 효과가 터지지 않고 순수하게 2스택을 쌓는다.
     * 이후 다른 오퍼레이터가 HEAVY_ATTACK 등으로 소모 가능.
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        // 물리 피해
        int damage = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(damage, hero, DamageType.PHYSICAL);

        if (!target.isAlive()) return;

        // 아츠 부착 침묵 소모 (폭발/반응 없이 detach — 조건 체크는 이미 통과)
        ArtsAttachment arts = target.buff(ArtsAttachment.class);
        if (arts != null) {
            arts.detach();
        }

        // 특수 띄우기: 방어불능 2스택 추가 (이상 발동 없음)
        DefenselessStack.addStackOnly(target);
        DefenselessStack.addStackOnly(target);
    }

    // ─────────────────────────────────────────────
    // 궁극기: 대량 열기 피해 + 열기 부착 (+늑대의 발톱 보유 시 추가 피해 + 회복)
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override
            public int maxCharge() { return 100; } // TODO: 수치 확정

            @Override
            public String name() { return "불꽃 분노"; } // TODO: 정식 스킬명 확정

            @Override
            public String description() {
                return "대량 열기 피해(" + ULT_MULT + "×) + 열기 부착.\n" +
                       "늑대의 발톱 보유 시: 추가 열기 피해(" + ULT_WOLF_BONUS_MULT + "×) + Hero 회복(최대HP×" + ULT_RECOVERY_RATIO + ").\n" +
                       "TODO: 수치 확정";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 늑대의 발톱 보유 여부를 피해 전에 확인 (피해로 적이 죽어도 보너스 지급 결정에 사용)
                boolean hadWolfClaw = target.buff(WolfClaw.class) != null;

                // 대량 열기 피해
                int damage = Math.round(hero.damageRoll() * ULT_MULT);
                target.damage(damage, hero, DamageType.HEAT);

                // 열기 아츠 부착
                if (target.isAlive()) {
                    ArtsAttachment.apply(target, ArtsAttachment.ArtsType.HEAT, hero);
                }

                // 늑대의 발톱 조건부 보너스
                if (hadWolfClaw) {
                    // 추가 열기 피해 (적이 살아 있을 때만)
                    if (target.isAlive()) {
                        int bonusDmg = Math.round(hero.damageRoll() * ULT_WOLF_BONUS_MULT);
                        target.damage(bonusDmg, hero, DamageType.HEAT);
                    }

                    // Hero 회복 (적 생사 무관 — 발톱이 있었다면 항상 회복)
                    int recover = Math.round(hero.HT * ULT_RECOVERY_RATIO);
                    hero.HP = Math.min(hero.HP + recover, hero.HT);
                    hero.sprite.showStatus(CharSprite.POSITIVE, "+" + recover);
                }
            }
        };
    }
}
