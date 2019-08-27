
<template>
  <div class="xrd-tab-max-width">
    <div>
      <subViewTitle :title="service.service_code" @close="close" />

      <template>
        <div class="cert-hash">{{$t('services.serviceParameters')}}</div>
      </template>
    </div>

    <div class="apply-to-all">
      <div class="apply-to-all-text">{{$t('services.applyToAll')}}</div>
    </div>

    <div class="edit-row">
      <div class="edit-title">
        {{$t('services.serviceUrl')}}
        <helpIcon :text="$t('services.urlTooltip')" />
      </div>

      <div class="edit-input">
        <v-text-field
          v-model="service.url"
          @input="setTouched()"
          single-line
          class="description-input"
          v-validate="'required|wsdlUrl'"
          data-vv-as="field"
          name="url_field"
          :error-messages="errors.collect('url_field')"
        ></v-text-field>
      </div>

      <v-checkbox @change="setTouched()" v-model="url_all" color="primary" class="table-checkbox"></v-checkbox>
    </div>

    <div class="edit-row">
      <div class="edit-title">
        {{$t('services.timeoutSec')}}
        <helpIcon :text="$t('services.timeoutTooltip')" />
      </div>
      <div class="edit-input">
        <v-text-field
          v-model="service.timeout"
          @input="setTouched()"
          single-line
          type="number"
          style="max-width: 200px;"
          v-validate="{ max_value: 1000, numeric: true, required: true, min_value: 0 }"
          data-vv-as="field"
          name="max_value_field"
          :error-messages="errors.collect('max_value_field')"
        ></v-text-field>
        <!-- 0 - 1000 -->
      </div>

      <v-checkbox
        @change="setTouched()"
        v-model="timeout_all"
        color="primary"
        class="table-checkbox"
      ></v-checkbox>
    </div>

    <div class="edit-row">
      <div class="edit-title">
        {{$t('services.verifyTls')}}
        <helpIcon :text="$t('services.tlsTooltip')" />
      </div>
      <div class="edit-input">
        <v-checkbox
          :disabled="!isHttps"
          @change="setTouched()"
          v-model="service.ssl_auth"
          color="primary"
          class="table-checkbox"
        ></v-checkbox>
        <!--
        <v-checkbox v-else color="primary" :disabled="true" class="table-checkbox"></v-checkbox>-->
      </div>

      <v-checkbox
        @change="setTouched()"
        v-model="ssl_auth_all"
        color="primary"
        class="table-checkbox"
      ></v-checkbox>
    </div>

    <div class="button-wrap">
      <v-btn
        round
        color="primary"
        class="xrd-big-button elevation-0"
        :disabled="disableSave"
        @click="save()"
      >{{$t('action.save')}}</v-btn>
    </div>

    <div class="group-members-row">
      <div class="row-title">{{$t('services.accessRights')}}</div>
      <div class="row-buttons">
        <v-btn
          outline
          color="primary"
          class="xrd-big-button"
          :disabled="!hasMembers"
          @click="removeAllMembers()"
        >{{$t('action.removeAll')}}</v-btn>
        <v-btn
          outline
          color="primary"
          class="xrd-big-button"
          @click="showAddMembersDialog()"
        >{{$t('services.addSubjects')}}</v-btn>
      </div>
    </div>

    <v-card flat>
      <table class="xrd-table group-members-table">
        <tr>
          <th>{{$t('localGroup.name')}}</th>
          <th>{{$t('localGroup.id')}}</th>
          <th></th>
        </tr>
        <template v-if="accessRights">
          <tr v-for="groupMember in accessRights" v-bind:key="groupMember.id">
            <td>{{groupMember.name}}</td>
            <td>{{groupMember.id}}</td>

            <td>
              <div class="button-wrap">
                <v-btn
                  small
                  outline
                  round
                  color="primary"
                  class="xrd-small-button"
                  @click="removeMember(groupMember)"
                >{{$t('action.remove')}}</v-btn>
              </div>
            </td>
          </tr>
        </template>
      </table>

      <div class="footer-buttons-wrap">
        <v-btn
          round
          color="primary"
          class="xrd-big-button elevation-0"
          @click="close()"
        >{{$t('action.close')}}</v-btn>
      </div>
    </v-card>

    <!-- Confirm dialog delete group -->
    <confirmDialog
      :dialog="confirmGroup"
      title="localGroup.deleteTitle"
      text="localGroup.deleteText"
      @cancel="confirmGroup = false"
      @accept="doDeleteGroup()"
    />

    <!-- Confirm dialog remove member -->
    <confirmDialog
      :dialog="confirmMember"
      title="localGroup.removeTitle"
      text="localGroup.removeText"
      @cancel="confirmMember = false"
      @accept="doRemoveMember()"
    />

    <!-- Confirm dialog remove all members -->
    <confirmDialog
      :dialog="confirmAllMembers"
      title="localGroup.removeAllTitle"
      text="localGroup.removeAllText"
      @cancel="confirmAllMembers = false"
      @accept="doRemoveAllMembers()"
    />

    <!-- Add new members dialog -->
    <addMembersDialog
      :dialog="addMembersDialogVisible"
      :filtered="[]"
      @cancel="closeMembersDialog"
      @membersAdded="membersAdded"
    />
  </div>
</template>


<script lang="ts">
import Vue from 'vue';
import _ from 'lodash';
import axios from 'axios';
import { mapGetters } from 'vuex';
import { Permissions } from '@/global';
import SubViewTitle from '@/components/SubViewTitle.vue';
import AddMembersDialog from '@/components/AddMembersDialog.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import HelpIcon from '@/components/HelpIcon.vue';
import { Service } from '@/types.ts';
import { isValidWsdlURL } from '@/util/helpers';

type NullableService = undefined | Service;

export default Vue.extend({
  components: {
    SubViewTitle,
    AddMembersDialog,
    ConfirmDialog,
    HelpIcon,
  },
  props: {
    serviceId: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      touched: false,
      confirmGroup: false,
      confirmMember: false,
      confirmAllMembers: false,
      selectedMember: undefined as NullableService,
      description: undefined,
      url: '',
      addMembersDialogVisible: false,
      timeout: 23,
      accessRights: [
        {
          id: 'GLOBALGROUP:DEV:security-server-owners',
          name: 'Mock 1 security server owners',
        },
        {
          id: 'GLOBALGROUP:DEV:security-server-owners',
          name: 'Mock security server owners',
        },
      ],
      url_all: false,
      timeout_all: false,
      ssl_auth_all: false,
      service: {
        id: '',
        code: '',
        timeout: 0,
        ssl_auth: true,
        url: '',
      } as Service,
    };
  },
  computed: {
    isHttps(): boolean {
      if (this.service.url.startsWith('https')) {
        return true;
      }
      return false;
    },
    hasMembers(): boolean {
      const tempAccessRights: any = this.group;

      if (
        tempAccessRights &&
        tempAccessRights.members &&
        tempAccessRights.members.length > 0
      ) {
        return true;
      }
      return false;
    },

    disableSave() {
      // service is undefined --> can't save
      if (!this.service) {
        return true;
      }

      // errors in form --> can's save
      if (this.errors.any()) {
        return true;
      }

      // one of the "apply all" is checked --> save
      /* if (this.ssl_auth_all || this.url_all || this.timeout_all) {
        return false;
      }
      */

      if (!this.touched) {
        return true;
      }
    },
  },

  methods: {
    close(): void {
      this.$router.go(-1);
    },

    save(): void {
      axios
        .patch(`/services/${this.service.id}`, {
          service: this.service,
          timeout_all: this.timeout_all,
          url_all: this.url_all,
          ssl_auth_all: this.ssl_auth_all,
        })
        .then((res) => {
          this.service = res.data;
          this.$bus.$emit('show-success', 'Service saved');
          this.$router.go(-1);
        })
        .catch((error) => {
          this.$bus.$emit('show-error', error.message);
        });
    },

    setTouched(): void {
      this.touched = true;
    },

    fetchData(serviceId: string): void {
      axios
        .get(`/services/${serviceId}`)
        .then((res) => {
          this.service = res.data;
        })
        .catch((error) => {
          this.$bus.$emit('show-error', error.message);
        });

      axios
        .get(`/services/${serviceId}/access-rights`)
        .then((res) => {
          //this.service = res.data;
          console.log(res.data);
        })
        .catch((error) => {
          this.$bus.$emit('show-error', error.message);
        });
    },

    showAddMembersDialog(): void {
      this.addMembersDialogVisible = true;
    },

    membersAdded(): void {
      this.addMembersDialogVisible = false;
      // this.fetchData(this.clientId);
    },

    closeMembersDialog(): void {
      this.addMembersDialogVisible = false;
    },

    removeAllMembers(): void {
      this.confirmAllMembers = true;
    },

    doRemoveAllMembers(): void {
      const ids: any = [];
      const tempAccessRights: any = this.group;
      tempAccessRights.members.forEach((member: any) => {
        ids.push(member.id);
      });

      this.removeArrayOfMembers(ids);
      this.confirmAllMembers = false;
    },

    removeMember(member: any): void {
      this.confirmMember = true;
      this.selectedMember = member;
    },
    doRemoveMember() {
      const member: Service = this.selectedMember as Service;

      if (member && member.id) {
        this.removeArrayOfMembers([member.id]);
      }

      this.confirmMember = false;
      this.selectedMember = undefined;
    },

    removeArrayOfMembers(members: any) {
      axios
        .post(`/groups/${this.groupId}/members/delete`, {
          items: members,
        })
        .catch((error) => {
          this.$bus.$emit('show-error', error.message);
        })
        .finally(() => {
          this.fetchData(this.clientId, this.groupId);
        });
    },

    deleteGroup(): void {
      this.confirmGroup = true;
    },
    doDeleteGroup(): void {
      this.confirmGroup = false;

      axios
        .delete(`/groups/${this.groupId}`)
        .then(() => {
          this.$bus.$emit('show-success', 'localGroup.groupDeleted');
          this.$router.go(-1);
        })
        .catch((error) => {
          this.$bus.$emit('show-error', error.message);
        });
    },
  },
  created() {
    this.fetchData(this.serviceId);
  },
});
</script>

<style lang="scss" scoped>
@import '../assets/colors';

.apply-to-all {
  display: flex;
  justify-content: flex-end;

  .apply-to-all-text {
    width: 100px;
  }
}

.edit-row {
  display: flex;
  align-items: baseline;
  //  border: 1px solid gray;

  .description-input {
    width: 100%;
    max-width: 450px;
  }

  .edit-title {
    display: flex;
    align-content: center;
    min-width: 200px;
    //    border: 1px solid green;
    margin-right: 20px;
  }

  .edit-input {
    display: flex;
    align-content: center;
    //    border: 1px solid blue;
    width: 100%;
  }
}

.edit-row > *:last-child {
  margin-left: 20px;
  width: 100px;
  max-width: 100px;
  min-width: 100px;
  //border: 1px solid red;
  margin-left: auto;
  margin-right: 0;
}

.delete-wrap {
  margin-top: 50px;
  display: flex;
  justify-content: flex-end;
}

.group-members-row {
  width: 100%;
  display: flex;
  margin-top: 70px;
  align-items: baseline;
}
.row-title {
  width: 100%;
  justify-content: space-between;
  color: #202020;
  font-family: Roboto;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0.5px;
}
.row-buttons {
  display: flex;
}

.wrapper {
  display: flex;
  justify-content: center;
  flex-direction: column;
  padding-top: 60px;
  height: 100%;
}

.cert-dialog-header {
  display: flex;
  justify-content: center;
  border-bottom: 1px solid #9b9b9b;
  color: #4a4a4a;
  font-family: Roboto;
  font-size: 34px;
  font-weight: 300;
  letter-spacing: 0.5px;
  line-height: 51px;
}

.cert-hash {
  margin-top: 50px;
  display: flex;
  justify-content: space-between;
  color: #202020;
  font-family: Roboto;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0.5px;
  line-height: 30px;
}

.group-members-table {
  margin-top: 10px;
  width: 100%;
  th {
    text-align: left;
  }
}

.button-wrap {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

.footer-buttons-wrap {
  margin-top: 48px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid $XRoad-Grey40;
  padding-top: 20px;
}
</style>
