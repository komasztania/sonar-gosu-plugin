/*
 * Copyright (C) 2023 FRIDAY Insurance S.A.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package de.friday.test.support;

import java.nio.file.Path;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.rule.RuleKey;

public class GosuSensorContextTester {
    private final SensorContextTester sensorContextTester;

    public GosuSensorContextTester(Path moduleBaseDir, String ruleKey) {
        this.sensorContextTester = create(moduleBaseDir, ruleKey);
    }

    private SensorContextTester create(Path moduleBaseDir, String ruleKey) {
        final ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder().create(RuleKey.of("gosu", ruleKey)).activate();
        final ActiveRules activeRules = activeRulesBuilder.build();
        return SensorContextTester.create(moduleBaseDir).setActiveRules(activeRules);
    }

    public SensorContextTester get() {
        return sensorContextTester;
    }
}