<template>
  <div class="row-wrap">
    <div :class="iconClass"></div>
    <div>{{$t(status)}}</div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';

export default Vue.extend({
  props: {
    certificate: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {};
  },
  computed: {
    status() {
      switch (this.certificate.status) {
        case 'SAVED':
          return 'keys.certStatus.saved';
          break;
        case 'REGISTRATION_IN_PROGRESS':
          return 'keys.certStatus.registration';
          break;
        case 'REGISTERED':
          return 'keys.certStatus.registered';
          break;
        case 'DELETION_IN_PROGRESS':
          return 'keys.certStatus.deletion';
          break;
        case 'GLOBAL_ERROR':
          return 'keys.certStatus.globalError';
          break;
        default:
          return '-';
          break;
      }
    },
    iconClass() {
      switch (this.certificate.status) {
        case 'SAVED':
          return 'status-green';
          break;
        default:
          return 'status-red';
          break;
      }
    },
  },
  methods: {},
});
</script>


<style lang="scss" scoped>
.row-wrap {
  display: flex;
  flex-direction: row;
  align-items: baseline;
}

%status-icon-shared {
  height: 8px;
  width: 8px;
  border-radius: 50%;
  margin-right: 16px;
}

.status-red {
  @extend %status-icon-shared;
  background: #d0021b;
}

.status-green {
  @extend %status-icon-shared;
  background: #7ed321;
}
</style>
