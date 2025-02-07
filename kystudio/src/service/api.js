import Vue from 'vue'
import VueResource from 'vue-resource'
import projectApi from './project'
import modelApi from './model'
import internalTableApi from './internalTable'
import configApi from './config'
import kafkaApi from './kafka'
import userApi from './user'
import systemApi from './system'
import datasourceApi from './datasource'
import monitorApi from './monitor'
import dashboardApi from './dashboard'
// console.log(base64)
Vue.use(VueResource)
export default {
  project: projectApi,
  model: modelApi,
  config: configApi,
  kafka: kafkaApi,
  user: userApi,
  system: systemApi,
  datasource: datasourceApi,
  monitor: monitorApi,
  dashboard: dashboardApi,
  internalTable: internalTableApi
}
