<template>
  <div>
    <v-card flat>
      Service clients
      <br />
      ID: {{id}}
    </v-card>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';

import { mapGetters } from 'vuex';
import { Permissions } from '@/global';

export default Vue.extend({
  components: {},
  props: {
    id: {
      type: String,
      required: true,
    },
  },
  data() {
    return {};
  },
  computed: {
    ...mapGetters(['client']),
  },
  methods: {},
});
</script>

