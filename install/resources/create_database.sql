/* Creates the srcalc database on MySQL. */

CREATE DATABASE srcalc;
USE srcalc;

create table boolean_variable (id integer not null, primary key (id));
create table cpt (id integer not null auto_increment, complexity varchar(40) not null, cpt_code varchar(5) not null, eligible boolean not null, long_description varchar(256) not null, rvu float not null, short_description varchar(256) not null, primary key (id));
create table discrete_numerical_var (units varchar(40) not null, lower_bound float not null, lower_inclusive boolean not null, upper_bound float not null, upper_inclusive boolean not null, id integer not null, primary key (id));
create table discrete_numerical_var_category (variable_id integer not null, option_value varchar(80) not null, upper_bound float not null, upper_inclusive boolean not null, primary key (variable_id, option_value, upper_bound, upper_inclusive));
create table historical_calc (id integer not null auto_increment, provider_type varchar(80), seconds_to_first_run integer not null, specialty_name varchar(100) not null, start_timestamp datetime not null, user_station varchar(10) not null, primary key (id));
create table multi_select_variable (display_type varchar(255), id integer not null, primary key (id));
create table multi_select_variable_option (variable_id integer not null, option_value varchar(80) not null, option_index integer not null, primary key (variable_id, option_index));
create table numerical_variable (units varchar(40) not null, lower_bound float not null, lower_inclusive boolean not null, upper_bound float not null, upper_inclusive boolean not null, id integer not null, primary key (id));
create table procedure_variable (id integer not null, primary key (id));
create table risk_model (id integer not null auto_increment, constant float, display_name varchar(80) not null, primary key (id));
create table risk_model_boolean_term (risk_model_id integer not null, variable integer not null, coefficient float not null, primary key (risk_model_id, variable, coefficient));
create table risk_model_derived_term (risk_model_id integer not null, rule integer, coefficient float not null, primary key (risk_model_id, rule, coefficient));
create table risk_model_discrete_term (risk_model_id integer not null, option_index integer not null, variable integer not null, coefficient float not null, primary key (risk_model_id, option_index, variable, coefficient));
create table risk_model_numerical_term (risk_model_id integer not null, variable integer not null, coefficient float not null, primary key (risk_model_id, variable, coefficient));
create table risk_model_procedure_term (risk_model_id integer not null, variable integer not null, coefficient float not null, primary key (risk_model_id, variable, coefficient));
create table rule (id integer not null auto_increment, bypass_enabled boolean not null, display_name varchar(80) not null, summand_expression varchar(255) not null, primary key (id));
create table rule_value_matcher (rule_id integer not null, boolean_expression varchar(255), expression_enabled boolean not null, variable integer);
create table signed_result (run_id integer not null, cpt_code varchar(5), patient_dfn integer not null, signature_timestamp datetime not null, primary key (run_id));
create table signed_result_input (result_id integer not null, variable_value varchar(255) not null, variable_key varchar(40) not null, primary key (result_id, variable_key));
create table signed_result_outcome (result_id integer not null, risk_result float not null, model_name varchar(80) not null, primary key (result_id, model_name));
create table specialty (id integer not null auto_increment, name varchar(100) not null, vista_id integer not null, primary key (id));
create table specialty_risk_model (specialty_id integer not null, risk_model_id integer not null, primary key (specialty_id, risk_model_id));
create table variable (id integer not null auto_increment, display_name varchar(80) not null, help_text varchar(4000), variable_key varchar(40) not null, retrieval_key integer, variable_group integer not null, primary key (id));
create table variable_group (id integer not null auto_increment, display_order integer not null, name varchar(255), primary key (id));
alter table variable add constraint UK_3on3hwgilp01pjk6iqxarybnm unique (variable_key);
alter table boolean_variable add index FK_8s7i3kftdcnt17a8us2sh6qou (id), add constraint FK_8s7i3kftdcnt17a8us2sh6qou foreign key (id) references variable (id);
alter table discrete_numerical_var add index FK_1hmr3q0o9tn8xd48slnkwk0ld (id), add constraint FK_1hmr3q0o9tn8xd48slnkwk0ld foreign key (id) references variable (id);
alter table discrete_numerical_var_category add index FK_ai58j7ktbfoadjqgqm0llm11e (variable_id), add constraint FK_ai58j7ktbfoadjqgqm0llm11e foreign key (variable_id) references discrete_numerical_var (id);
alter table multi_select_variable add index FK_18hqfsy87bg9ucro7r0h6hl5t (id), add constraint FK_18hqfsy87bg9ucro7r0h6hl5t foreign key (id) references variable (id);
alter table multi_select_variable_option add index FK_aho8l3stxs2pix74vg19xmman (variable_id), add constraint FK_aho8l3stxs2pix74vg19xmman foreign key (variable_id) references multi_select_variable (id);
alter table numerical_variable add index FK_ntgiooontcrixh9umumtbf9yh (id), add constraint FK_ntgiooontcrixh9umumtbf9yh foreign key (id) references variable (id);
alter table procedure_variable add index FK_liut45a9x2av2o51y248r1s7d (id), add constraint FK_liut45a9x2av2o51y248r1s7d foreign key (id) references variable (id);
alter table risk_model_boolean_term add index FK_dubx7eop6473bngo3ner4sgn8 (variable), add constraint FK_dubx7eop6473bngo3ner4sgn8 foreign key (variable) references boolean_variable (id);
alter table risk_model_boolean_term add index FK_1opepgms7fajg9ak0rtducmrh (risk_model_id), add constraint FK_1opepgms7fajg9ak0rtducmrh foreign key (risk_model_id) references risk_model (id);
alter table risk_model_derived_term add index FK_p0iglx6kntgl1tjkq9qj1elve (rule), add constraint FK_p0iglx6kntgl1tjkq9qj1elve foreign key (rule) references rule (id);
alter table risk_model_derived_term add index FK_n1ajl0qhkjo4h2xt6sqi97hcl (risk_model_id), add constraint FK_n1ajl0qhkjo4h2xt6sqi97hcl foreign key (risk_model_id) references risk_model (id);
alter table risk_model_discrete_term add index FK_drebha4havw164gg60a4284jo (variable), add constraint FK_drebha4havw164gg60a4284jo foreign key (variable) references variable (id);
alter table risk_model_discrete_term add index FK_ocwpgqfi5jno9sjq7vjcryx0a (risk_model_id), add constraint FK_ocwpgqfi5jno9sjq7vjcryx0a foreign key (risk_model_id) references risk_model (id);
alter table risk_model_numerical_term add index FK_ogw6j50qeggthcxlnfltgbmpk (variable), add constraint FK_ogw6j50qeggthcxlnfltgbmpk foreign key (variable) references numerical_variable (id);
alter table risk_model_numerical_term add index FK_s2hf68il2igkjcjktgrkrnbjf (risk_model_id), add constraint FK_s2hf68il2igkjcjktgrkrnbjf foreign key (risk_model_id) references risk_model (id);
alter table risk_model_procedure_term add index FK_6qew8kqxs6kjujfnjblxpa45c (variable), add constraint FK_6qew8kqxs6kjujfnjblxpa45c foreign key (variable) references procedure_variable (id);
alter table risk_model_procedure_term add index FK_d3tmxsvauxneoplv06nwqro2j (risk_model_id), add constraint FK_d3tmxsvauxneoplv06nwqro2j foreign key (risk_model_id) references risk_model (id);
alter table rule_value_matcher add index FK_3hhhc928hdy3o0beed9wjnq9m (variable), add constraint FK_3hhhc928hdy3o0beed9wjnq9m foreign key (variable) references variable (id);
alter table rule_value_matcher add index FK_rnatr4b2fmd3kx6ff0ka1g4le (rule_id), add constraint FK_rnatr4b2fmd3kx6ff0ka1g4le foreign key (rule_id) references rule (id);
alter table signed_result add index FK_fbh9rjcin6qo4vi5cyhvqneok (run_id), add constraint FK_fbh9rjcin6qo4vi5cyhvqneok foreign key (run_id) references historical_calc (id);
alter table signed_result_input add index FK_a7w1dts524sqt755yf1q8igvv (result_id), add constraint FK_a7w1dts524sqt755yf1q8igvv foreign key (result_id) references signed_result (run_id);
alter table signed_result_outcome add index FK_jbofpapc0i5qoixu28f4ekjm (result_id), add constraint FK_jbofpapc0i5qoixu28f4ekjm foreign key (result_id) references signed_result (run_id);
alter table specialty_risk_model add index FK_g44r1aagpmd130bpvefwj08ve (risk_model_id), add constraint FK_g44r1aagpmd130bpvefwj08ve foreign key (risk_model_id) references risk_model (id);
alter table specialty_risk_model add index FK_rrt5htbg4mmbygm2qieptls2q (specialty_id), add constraint FK_rrt5htbg4mmbygm2qieptls2q foreign key (specialty_id) references specialty (id);
alter table variable add index FK_thvnglkbf1ynftxe54elpdfd7 (variable_group), add constraint FK_thvnglkbf1ynftxe54elpdfd7 foreign key (variable_group) references variable_group (id);

GRANT ALL PRIVILEGES ON srcalc.* TO 'srcalc'@'localhost';
