/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.endfield;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charging;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BruteSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * 본 크러셔의 주먹 '로댄' (Rodan) — 4번 협곡 1계층(A5) 보스.
 *
 * ── 기본 수치 ─────────────────────────────
 *   HP    : 120
 *   ATK   : 100 (→ damageRoll)          TODO: 최종 확정
 *   명중  : 15                           TODO: 미확정
 *   회피  : 10                           TODO: 미확정
 *   방어  : 6 (물리/열기/냉기/자연/전기) TODO: 최종 확정
 *   EXP   : 6  / maxLvl : 9
 *   드랍  : WornKey(100%) + Gold(100%)
 *
 * ── 패턴 ──────────────────────────────────
 *   [차지] 포격 (Bombardment)
 *     - 조건: 이전 턴에 피해를 받았을 때 40% 확률 (쿨타임 5턴 / 페이즈2: 10턴)
 *     - 동작: 3턴 차지 후 차지 시작 시 플레이어 위치 3×3 범위에 물리 피해
 *
 *   [즉발 + 집중] 강렬한 포효 (Fierce Roar)  — 페이즈2 진입 시 1회
 *     - 즉발: 주변 3×3 플레이어를 2칸 넉백
 *     - 집중 3턴 후: 로댄 기준 7×7 범위에 강한 물리 피해
 *
 *   [집중] 화염방사 (Flamethrower)
 *     - 조건: 페이즈2, 플레이어와 거리 ≥ 3, 50% 확률 (쿨타임 5턴)
 *     - 동작: 1턴 집중 후 2턴간 플레이어 방향 3×5 범위에 열기 피해
 *
 * ── 페이즈2 ───────────────────────────────
 *   HP ≤ 60 (50%) 시: ATK +20, 방어 +2, 포격 쿨타임 10턴으로 증가
 *   강렬한 포효 즉시 발동 (1회)
 */
public class Rodan extends Mob {

    // ── 패턴 상태 상수 ──────────────────────
    private static final int PAT_NONE        = 0;
    private static final int PAT_BOMB_CHARGE = 1; // 포격 차지 중 (timer > 0: 대기, = 0: 발사)
    private static final int PAT_ROAR_FOCUS  = 2; // 강렬한 포효 집중 중
    private static final int PAT_FLAME_FIRE  = 3; // 화염방사 발사 중

    // ── 수치 (TODO: 확정) ───────────────────
    private static final int BASE_ATK      = 100;
    private static final int PHASE2_ATK    = 120;  // ATK +20
    private static final int BASE_DR       = 6;
    private static final int PHASE2_DR     = 8;    // 방어 +2
    private static final int BOMB_DMG_MIN  = 30;   // 포격 피해 최소    TODO
    private static final int BOMB_DMG_MAX  = 60;   // 포격 피해 최대    TODO
    private static final int ROAR_DMG_MIN  = 70;   // 포효 피해 최소    TODO
    private static final int ROAR_DMG_MAX  = 100;  // 포효 피해 최대    TODO
    private static final int FLAME_DMG_MIN = 30;   // 화염방사 피해 최소 TODO
    private static final int FLAME_DMG_MAX = 55;   // 화염방사 피해 최대 TODO

    // 쿨타임
    private static final int BOMB_CD_P1    = 5;
    private static final int BOMB_CD_P2    = 10;
    private static final int FLAME_CD      = 5;

    // 차지/집중 턴 수
    private static final int BOMB_CHARGE_TURNS  = 3; // 포격 차지 턴 수
    private static final int ROAR_FOCUS_TURNS   = 3; // 강렬한 포효 집중 턴 수
    // 화염방사: 집중 1턴(startFlamethrower 자체 소비) + FLAME_FIRE_TURNS 발사
    private static final int FLAME_FIRE_TURNS   = 2;

    // ── 상태 변수 ──────────────────────────
    private int     currentPattern  = PAT_NONE;
    private int     patternTimer    = 0;
    private boolean phase2          = false;
    private boolean roarDone        = false;
    private boolean hitLastTurn     = false;
    private int     bombCooldown    = 0;
    private int     flameCooldown   = 0;
    private int     bombTarget      = -1; // 포격 대상 셀 (차지 시작 시 고정)
    private int     flameDir        = 0;  // 화염방사 주 방향 오프셋 (+1/-1/+W/-W)

    {
        spriteClass  = BruteSprite.class; // TODO: 전용 스프라이트로 교체
        HP = HT      = 120;
        defenseSkill = 10; // TODO: DB에 명중/회피 미정
        EXP          = 6;
        maxLvl       = 9;

        properties.add(Property.BOSS);
        properties.add(Property.LARGE);
    }

    // ── 기본 능력치 ─────────────────────────

    @Override
    public int damageRoll() {
        int max = phase2 ? PHASE2_ATK : BASE_ATK;
        return Random.NormalIntRange(max / 2, max);
    }

    @Override
    public int attackSkill(Char target) {
        return 15; // TODO: DB 미확정
    }

    @Override
    public int drRoll() {
        int dr = phase2 ? PHASE2_DR : BASE_DR;
        return super.drRoll() + Random.NormalIntRange(0, dr);
    }

    // ── 피해 수신 (hitLastTurn 추적 + 페이즈2 체크) ─

    @Override
    public void damage(int dmg, Object src, DamageType type) {
        super.damage(dmg, src, type);
        if (isAlive()) {
            hitLastTurn = true;
        }
    }

    // ── 행동 루프 ────────────────────────────

    @Override
    protected boolean act() {

        // 보스 체력바 등록 (최초 1회)
        if (!BossHealthBar.isAssigned()) {
            BossHealthBar.assignBoss(this);
            Dungeon.level.seal();
        }

        // 쿨타임 감소
        if (bombCooldown  > 0) bombCooldown--;
        if (flameCooldown > 0) flameCooldown--;

        // 페이즈2 진입 체크 (act 시작 시점에 확인)
        if (!phase2 && HP <= HT / 2 && HP > 0) {
            enterPhase2();
            return true; // enterPhase2 내부에서 spend(TICK) 처리
        }

        // 진행 중인 패턴 처리
        if (currentPattern != PAT_NONE) {
            return tickPattern();
        }

        // 패턴 선택 (HUNTING 중일 때만)
        if (state == HUNTING && enemy != null) {

            // 포격: 이전 턴에 피해를 받았고, 쿨타임 완료, 40% 확률
            if (hitLastTurn && bombCooldown == 0 && Random.Float() < 0.4f) {
                hitLastTurn = false;
                return startBombardment();
            }

            // 화염방사: 페이즈2, 거리 ≥ 3, 쿨타임 완료, 50% 확률
            if (phase2 && flameCooldown == 0
                    && Dungeon.level.distance(pos, enemy.pos) >= 3
                    && Random.Float() < 0.5f) {
                return startFlamethrower();
            }
        }

        hitLastTurn = false;
        return super.act();
    }

    // ── 페이즈2 진입 ─────────────────────────

    private void enterPhase2() {
        phase2 = true;

        // 보스 체력바 출혈 표시
        BossHealthBar.bleed(true);

        // 강렬한 포효 즉발 넉백 (로댄 기준 3×3 안의 플레이어)
        if (Dungeon.level.distance(pos, Dungeon.hero.pos) <= 1) {
            doRoarKnockback();
        }

        // 7×7 경고 표시 (집중 시작)
        showRoarWarning();

        if (Dungeon.level.heroFOV[pos]) {
            yell(Messages.get(this, "phase2"));
        }

        currentPattern = PAT_ROAR_FOCUS;
        patternTimer   = ROAR_FOCUS_TURNS; // 초기값: 3턴 집중

        spend(TICK);
    }

    // ── 패턴 tick 처리 ───────────────────────

    private boolean tickPattern() {
        switch (currentPattern) {

            // ── 포격 차지 ──
            case PAT_BOMB_CHARGE:
                if (patternTimer > 0) {
                    patternTimer--;
                    showBombWarning();
                    spend(TICK);
                    return true;
                } else {
                    // 발사
                    executeBombardment();
                    Buff.detach(this, Charging.class);
                    currentPattern = PAT_NONE;
                    bombCooldown   = phase2 ? BOMB_CD_P2 : BOMB_CD_P1;
                    spend(TICK);
                    return true;
                }

            // ── 강렬한 포효 집중 ──
            case PAT_ROAR_FOCUS:
                if (patternTimer > 0) {
                    patternTimer--;
                    showRoarWarning();
                    spend(TICK);
                    return true;
                } else {
                    // 발사
                    executeRoar();
                    currentPattern = PAT_NONE;
                    roarDone       = true;
                    spend(TICK);
                    return true;
                }

            // ── 화염방사 발사 중 ──
            // patternTimer: 남은 발사 횟수. 발사 후 decrement. 0이 되면 종료.
            case PAT_FLAME_FIRE:
                executeFlame();
                patternTimer--;
                if (patternTimer <= 0) {
                    currentPattern = PAT_NONE;
                    flameCooldown  = FLAME_CD;
                }
                spend(TICK);
                return true;

            default:
                currentPattern = PAT_NONE;
                return super.act();
        }
    }

    // ── 패턴 시작 ────────────────────────────

    private boolean startBombardment() {
        bombTarget = enemy.pos; // 플레이어 위치 고정

        // 차지 버프 부여 (연계기 트리거)
        Charging.startCharge(this);

        // 경고 표시 (차지 시작 첫 턴)
        showBombWarning();

        if (Dungeon.level.heroFOV[pos]) {
            sprite.showStatus(CharSprite.WARNING, Messages.get(this, "bomb_charge"));
        }

        currentPattern = PAT_BOMB_CHARGE;
        patternTimer   = BOMB_CHARGE_TURNS - 1; // 이번 턴 포함해서 3턴 총 소모
        spend(TICK);
        return true;
    }

    private boolean startFlamethrower() {
        // 집중 시작 시 플레이어 방향 고정
        computeFlameDir();
        showFlameWarning();

        if (Dungeon.level.heroFOV[pos]) {
            sprite.showStatus(CharSprite.WARNING, Messages.get(this, "flame_focus"));
        }

        // 집중 1턴 자체가 이 act()에서 소비됨 → 다음 act()부터 2턴 발사
        currentPattern = PAT_FLAME_FIRE;
        patternTimer   = FLAME_FIRE_TURNS; // 2
        spend(TICK);
        return true;
    }

    // ── 포격 실행 ────────────────────────────

    private void executeBombardment() {
        if (!Dungeon.level.heroFOV[pos] && !Dungeon.level.heroFOV[bombTarget]) return;

        PixelScene.shake(2, 0.3f);

        int W = Dungeon.level.width();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cell = bombTarget + dy * W + dx;
                if (!validCell(cell)) continue;

                Char ch = Actor.findChar(cell);
                if (ch != null && ch != this) {
                    ch.damage(Random.NormalIntRange(BOMB_DMG_MIN, BOMB_DMG_MAX),
                              this, DamageType.PHYSICAL);
                }
            }
        }

        if (Dungeon.level.heroFOV[pos]) {
            GLog.w(Messages.get(this, "bomb_fire"));
        }
    }

    // ── 강렬한 포효 실행 ─────────────────────

    /** 즉발 넉백: 로댄 기준 3×3 안의 플레이어를 2칸 뒤로. */
    private void doRoarKnockback() {
        Hero hero    = Dungeon.hero;
        int  heroPos = hero.pos;
        int  oppositeAdjacent = heroPos + (heroPos - pos);

        Ballistica traj = new Ballistica(heroPos, oppositeAdjacent, Ballistica.MAGIC_BOLT);
        WandOfBlastWave.throwChar(hero, traj, 2, false, false, this);
        if (hero.isAlive()) hero.interrupt();
    }

    /** 집중 완료 시 7×7 범위 강타. */
    private void executeRoar() {
        if (!Dungeon.level.heroFOV[pos]) return;

        PixelScene.shake(4, 0.5f);

        int W = Dungeon.level.width();
        for (int dy = -3; dy <= 3; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                int cell = pos + dy * W + dx;
                if (!validCell(cell)) continue;

                Char ch = Actor.findChar(cell);
                if (ch != null && ch != this) {
                    ch.damage(Random.NormalIntRange(ROAR_DMG_MIN, ROAR_DMG_MAX),
                              this, DamageType.PHYSICAL);
                }
            }
        }

        GLog.w(Messages.get(this, "roar_fire"));
    }

    // ── 화염방사 실행 ────────────────────────

    /** 집중 시작 시 플레이어 방향(4방향 중 우세한 방향)을 flameDir로 고정. */
    private void computeFlameDir() {
        if (enemy == null) { flameDir = 1; return; }
        int W   = Dungeon.level.width();
        int dr  = (enemy.pos / W) - (pos / W); // 행 차이
        int dc  = (enemy.pos % W) - (pos % W); // 열 차이
        if (Math.abs(dr) >= Math.abs(dc)) {
            flameDir = (dr >= 0) ? W : -W; // 아래 or 위
        } else {
            flameDir = (dc >= 0) ? 1 : -1; // 오른쪽 or 왼쪽
        }
    }

    /** 화염방사 1턴 발사: 3×5 범위 열기 피해. */
    private void executeFlame() {
        int W       = Dungeon.level.width();
        boolean vert = (Math.abs(flameDir) == W);

        for (int depth = 1; depth <= 5; depth++) {
            for (int perp = -1; perp <= 1; perp++) {
                int cell;
                if (vert) {
                    cell = pos + depth * flameDir + perp;
                } else {
                    cell = pos + depth * flameDir + perp * W;
                }
                if (!validCell(cell)) continue;

                Char ch = Actor.findChar(cell);
                if (ch != null && ch != this) {
                    // 화염방사는 열기 피해만, 열기 상태이상 부여 없음 (DB 명시)
                    ch.damage(Random.NormalIntRange(FLAME_DMG_MIN, FLAME_DMG_MAX),
                              this, DamageType.HEAT);
                }
            }
        }
    }

    // ── 경고 표시 ────────────────────────────

    private void showBombWarning() {
        if (bombTarget < 0 || sprite == null || sprite.parent == null) return;
        int W = Dungeon.level.width();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cell = bombTarget + dy * W + dx;
                if (validCell(cell)) {
                    sprite.parent.add(new TargetedCell(cell, 0xFF4400)); // 주황빛 경고
                }
            }
        }
    }

    private void showRoarWarning() {
        if (sprite == null || sprite.parent == null) return;
        int W = Dungeon.level.width();
        for (int dy = -3; dy <= 3; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                int cell = pos + dy * W + dx;
                if (validCell(cell)) {
                    sprite.parent.add(new TargetedCell(cell, 0xFF0000)); // 붉은 경고
                }
            }
        }
    }

    private void showFlameWarning() {
        if (sprite == null || sprite.parent == null || flameDir == 0) return;
        int W       = Dungeon.level.width();
        boolean vert = (Math.abs(flameDir) == W);
        for (int depth = 1; depth <= 5; depth++) {
            for (int perp = -1; perp <= 1; perp++) {
                int cell;
                if (vert) {
                    cell = pos + depth * flameDir + perp;
                } else {
                    cell = pos + depth * flameDir + perp * W;
                }
                if (validCell(cell)) {
                    sprite.parent.add(new TargetedCell(cell, 0xFF8800)); // 불꽃 경고
                }
            }
        }
    }

    // ── 유틸 ─────────────────────────────────

    /** 셀이 맵 범위 내에 있는지 확인. */
    private boolean validCell(int cell) {
        return cell >= 0 && cell < Dungeon.level.length();
    }

    // ── 사망 처리 ────────────────────────────

    @Override
    public void die(Object cause) {
        Dungeon.level.unseal();
        super.die(cause);

        // 5층 열쇠 + 크레디트 드랍
        Dungeon.level.drop(new WornKey(Dungeon.depth), pos).sprite.drop();
        Dungeon.level.drop(new Gold(Random.NormalIntRange(50, 100)), pos).sprite.drop();

        Buff.detach(this, Charging.class);
    }

    // ── 직렬화 ───────────────────────────────

    private static final String KEY_PAT        = "pattern";
    private static final String KEY_PAT_TIMER  = "pat_timer";
    private static final String KEY_PHASE2     = "phase2";
    private static final String KEY_ROAR_DONE  = "roar_done";
    private static final String KEY_HIT_LAST   = "hit_last_turn";
    private static final String KEY_BOMB_CD    = "bomb_cd";
    private static final String KEY_FLAME_CD   = "flame_cd";
    private static final String KEY_BOMB_TGT   = "bomb_target";
    private static final String KEY_FLAME_DIR  = "flame_dir";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(KEY_PAT,       currentPattern);
        bundle.put(KEY_PAT_TIMER, patternTimer);
        bundle.put(KEY_PHASE2,    phase2);
        bundle.put(KEY_ROAR_DONE, roarDone);
        bundle.put(KEY_HIT_LAST,  hitLastTurn);
        bundle.put(KEY_BOMB_CD,   bombCooldown);
        bundle.put(KEY_FLAME_CD,  flameCooldown);
        bundle.put(KEY_BOMB_TGT,  bombTarget);
        bundle.put(KEY_FLAME_DIR, flameDir);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        currentPattern = bundle.getInt(KEY_PAT);
        patternTimer   = bundle.getInt(KEY_PAT_TIMER);
        phase2         = bundle.getBoolean(KEY_PHASE2);
        roarDone       = bundle.getBoolean(KEY_ROAR_DONE);
        hitLastTurn    = bundle.getBoolean(KEY_HIT_LAST);
        bombCooldown   = bundle.getInt(KEY_BOMB_CD);
        flameCooldown  = bundle.getInt(KEY_FLAME_CD);
        bombTarget     = bundle.getInt(KEY_BOMB_TGT);
        flameDir       = bundle.getInt(KEY_FLAME_DIR);
    }
}
