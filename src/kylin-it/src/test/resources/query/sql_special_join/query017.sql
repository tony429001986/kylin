--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

select fact.cal_dt, sum(fact.price) from test_kylin_fact fact 
left join edw.test_cal_dt cal
	on fact.cal_dt=cal.cal_dt
inner join
(
	select test_kylin_fact.cal_dt, count(1) from test_kylin_fact left join edw.test_cal_dt
	on test_kylin_fact.cal_dt=test_cal_dt.cal_dt group by test_kylin_fact.cal_dt order by 2 desc limit 4
) cal_2 
	on fact.cal_dt = cal_2.cal_dt 
group by fact.cal_dt
