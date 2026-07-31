/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Endministrator;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.ChenQianyu;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Catcher;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.DaPan;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Pogranichnik;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Rossi;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Lifeng;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.LastLight;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Snowshine;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Yvonne;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Alesh;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Estella;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Tangtang;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Xaihi;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Gilberta;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Adelia;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Flurite;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Laevatain;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Ember;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Wulfgard;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Akekuri;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Avywenna;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Arclight;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Perlica;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Antal;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 오퍼레이터 영구 데이터 레지스트리.
 *
 * 런을 넘어 영구적으로 유지되는 데이터를 관리한다.
 * - 메인 오퍼레이터로 해금된 오퍼레이터 목록
 *   (해금 조건: 팀 오퍼레이터로 1회 이상 편성)
 *
 * 저장 파일: {@link #REGISTRY_FILE}
 * Badges.java 방식과 동일하게 전역 Bundle 파일로 관리.
 *
 * [첫 번째 런 정책]
 * 저장 파일이 없으면 (첫 실행) DEFAULT_UNLOCKED 목록을 자동 해금한다.
 * 기본 해금 오퍼레이터: 관리자 (Endministrator), 펠리카 (Perlica)
 * 이후 팀 오퍼레이터 편성 이력이 쌓이면서 해금 풀이 확장된다.
 */
public class OperatorRegistry {

    public static final String REGISTRY_FILE = "operators.dat";

    // ─────────────────────────────────────────────
    // 전체 오퍼레이터 로스터
    // 오퍼레이터 구현 완료 시 이 목록에 추가
    // ─────────────────────────────────────────────

    public static final List<Class<? extends Operator>> ALL_OPERATORS = new ArrayList<>();

    static {
        ALL_OPERATORS.add(Endministrator.class);
        ALL_OPERATORS.add(ChenQianyu.class);
        ALL_OPERATORS.add(Lifeng.class);
        ALL_OPERATORS.add(Pogranichnik.class);
        ALL_OPERATORS.add(DaPan.class);
        ALL_OPERATORS.add(Catcher.class);
        ALL_OPERATORS.add(Rossi.class);
        ALL_OPERATORS.add(Yvonne.class);
        ALL_OPERATORS.add(LastLight.class);
        ALL_OPERATORS.add(Snowshine.class);
        ALL_OPERATORS.add(Xaihi.class);
        ALL_OPERATORS.add(Alesh.class);
        ALL_OPERATORS.add(Estella.class);
        ALL_OPERATORS.add(Tangtang.class);
        ALL_OPERATORS.add(Gilberta.class);
        ALL_OPERATORS.add(Adelia.class);
        ALL_OPERATORS.add(Flurite.class);
        ALL_OPERATORS.add(Laevatain.class);
        ALL_OPERATORS.add(Ember.class);
        ALL_OPERATORS.add(Wulfgard.class);
        ALL_OPERATORS.add(Akekuri.class);
        ALL_OPERATORS.add(Avywenna.class);
        ALL_OPERATORS.add(Arclight.class);
        ALL_OPERATORS.add(Perlica.class);
        ALL_OPERATORS.add(Antal.class);
        // TODO: 오퍼레이터 구현 완료 시 순서대로 추가
        // ... (25명 전체)
    }

    // ─────────────────────────────────────────────
    // 기본 해금 오퍼레이터
    // ─────────────────────────────────────────────

    /**
     * 기본 해금 오퍼레이터 목록.
     * 처음 게임을 시작할 때 자동으로 해금된다.
     */
    private static final List<Class<? extends Operator>> DEFAULT_UNLOCKED = new ArrayList<>();

    static {
        // 테스트 빌드 로스터 — 스플래시 아트가 제작된 8명.
        // 아트가 추가되는 대로 순차 확대 예정 (최종 목표: 25명 전원)
        DEFAULT_UNLOCKED.add(Endministrator.class);  // 관리자
        DEFAULT_UNLOCKED.add(Perlica.class);          // 펠리카
        DEFAULT_UNLOCKED.add(ChenQianyu.class);     // 진천우
        DEFAULT_UNLOCKED.add(Akekuri.class);         // 아케쿠리
        DEFAULT_UNLOCKED.add(Estella.class);         // 에스텔라
        DEFAULT_UNLOCKED.add(Gilberta.class);        // 질베르타
        DEFAULT_UNLOCKED.add(Laevatain.class);        // 레바테인
        DEFAULT_UNLOCKED.add(Yvonne.class);          // 이본
    }

    // ─────────────────────────────────────────────
    // 메인 해금 데이터
    // ─────────────────────────────────────────────

    /** 메인 오퍼레이터로 해금된 오퍼레이터 클래스명 집합. null = 아직 미로드 */
    private static HashSet<String> unlockedAsMain = null;

    private static boolean saveNeeded = false;

    // ─────────────────────────────────────────────
    // 전역 저장/불러오기 (Badges 패턴)
    // ─────────────────────────────────────────────

    private static final String KEY_UNLOCKED = "unlockedAsMain";

    public static void loadGlobal() {
        if (unlockedAsMain == null) {
            try {
                Bundle bundle = FileUtils.bundleFromFile(REGISTRY_FILE);
                unlockedAsMain = restoreUnlocked(bundle);
            } catch (IOException e) {
                // 저장 파일 없음 = 첫 실행
                unlockedAsMain = new HashSet<>();
            }

            // 기본 해금 오퍼레이터는 항상 보장한다.
            // (기존 세이브에 신규 기본 해금 오퍼레이터를 추가했을 때도 소급 적용)
            boolean changed = false;
            for (Class<? extends Operator> opClass : DEFAULT_UNLOCKED) {
                if (unlockedAsMain.add(opClass.getSimpleName())) {
                    changed = true;
                }
            }
            if (changed) {
                saveNeeded = true;
                saveGlobal();
            }
        }
    }

    public static void saveGlobal() {
        if (saveNeeded) {
            Bundle bundle = new Bundle();
            storeUnlocked(bundle, unlockedAsMain);
            try {
                FileUtils.bundleToFile(REGISTRY_FILE, bundle);
                saveNeeded = false;
            } catch (IOException e) {
                ShatteredPixelDungeon.reportException(e);
            }
        }
    }

    private static HashSet<String> restoreUnlocked(Bundle bundle) {
        HashSet<String> set = new HashSet<>();
        if (bundle == null) return set;
        String[] names = bundle.getStringArray(KEY_UNLOCKED);
        if (names == null) return set;
        for (String name : names) {
            set.add(name);
        }
        return set;
    }

    private static void storeUnlocked(Bundle bundle, HashSet<String> set) {
        String[] names = set.toArray(new String[0]);
        bundle.put(KEY_UNLOCKED, names);
    }

    // ─────────────────────────────────────────────
    // 해금 관리
    // ─────────────────────────────────────────────

    /**
     * 오퍼레이터를 메인 오퍼레이터로 해금한다.
     * 팀 오퍼레이터로 편성될 때 호출.
     */
    public static void unlockAsMain(Class<? extends Operator> opClass) {
        loadGlobal();
        if (unlockedAsMain.add(opClass.getSimpleName())) {
            saveNeeded = true;
            saveGlobal();
        }
    }

    /**
     * 해당 오퍼레이터가 메인으로 해금되어 있는지 여부.
     */
    public static boolean isUnlockedAsMain(Class<? extends Operator> opClass) {
        loadGlobal();
        return unlockedAsMain.contains(opClass.getSimpleName());
    }

    // ─────────────────────────────────────────────
    // 헤드헌팅 후보 풀
    // ─────────────────────────────────────────────

    /**
     * 현재 파티에 없는 오퍼레이터 클래스 목록을 반환한다.
     * 헤드헌팅 허가증 사용 시 이 목록에서 랜덤 4명을 제시.
     *
     * @param hero 현재 Hero (파티 정보 참조용)
     */
    public static List<Class<? extends Operator>> getRecruitPool(Hero hero) {
        List<Class<? extends Operator>> pool = new ArrayList<>();

        // 현재 파티 구성원 클래스를 수집
        HashSet<String> inParty = new HashSet<>();
        if (hero.activeMainOperator != null) {
            inParty.add(hero.activeMainOperator.getClass().getSimpleName());
        }
        for (TeamOperator op : hero.teamOperators) {
            inParty.add(op.getClass().getSimpleName());
        }

        // 파티에 없는 오퍼레이터만 후보로 추가
        for (Class<? extends Operator> opClass : ALL_OPERATORS) {
            if (!inParty.contains(opClass.getSimpleName())) {
                pool.add(opClass);
            }
        }
        return pool;
    }

    /**
     * 메인으로 해금된 오퍼레이터 클래스 목록.
     * 게임 시작 시 메인 오퍼레이터 선택 화면에 사용.
     *
     * 첫 런이면 ALL_OPERATORS 전체를 반환.
     */
    public static List<Class<? extends Operator>> getAvailableAsMain() {
        loadGlobal();
        List<Class<? extends Operator>> available = new ArrayList<>();
        for (Class<? extends Operator> opClass : ALL_OPERATORS) {
            if (isUnlockedAsMain(opClass)) {
                available.add(opClass);
            }
        }
        return available;
    }
}
