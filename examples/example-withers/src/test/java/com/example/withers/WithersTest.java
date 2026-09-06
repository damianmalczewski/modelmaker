package com.example.withers;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.dto.Point;
import com.example.dto.Profile;
import org.junit.jupiter.api.Test;

/** {@code modelmaker.withers = true}: a {@code withXyz(value)} single-field copy per property. */
class WithersTest {

  @Test
  void withersCopyASingleFieldAndLeaveTheOriginalUntouched() {
    Point origin = Point.builder().x(0).y(0).build();

    Point moved = origin.withX(5);

    assertThat(moved.getX()).isEqualTo(5);
    assertThat(moved.getY()).isEqualTo(0);
    assertThat(origin.getX()).isEqualTo(0);
    assertThat(moved).isNotSameAs(origin).isNotEqualTo(origin);
  }

  @Test
  void aWitherThatDoesNotChangeTheValueStillReturnsANewEqualInstance() {
    Point origin = Point.builder().x(1).y(2).build();

    Point same = origin.withX(1);

    assertThat(same).isNotSameAs(origin).isEqualTo(origin);
  }

  @Test
  void witherWorksOnAnOptionalFieldToo() {
    Profile profile = Profile.builder().id("U1").build();
    assertThat(profile.getNickname()).isNull();

    Profile withNickname = profile.withNickname("neo");

    assertThat(withNickname.getNickname()).isEqualTo("neo");
    assertThat(profile.getNickname()).isNull();
    assertThat(withNickname).isNotSameAs(profile).isNotEqualTo(profile);
  }
}
