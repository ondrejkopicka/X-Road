<template>
  <div class="content">
    <ValidationObserver ref="form2" v-slot="{ validate, invalid }">
      <div v-for="item in csrForm" v-bind:key="item.id" class="row-wrap">
        <div class="label">{{$t('certificateProfile.' + item.label_key)}}</div>

        <div>
          <ValidationProvider
            :name="item.id"
            :rules="(item.required) && 'required' "
            v-slot="{ errors }"
          >
            <v-text-field
              class="form-input"
              :name="item.id"
              type="text"
              v-model="item.default_value"
              :disabled="item.read_only"
              :error-messages="errors"
            ></v-text-field>
          </ValidationProvider>
        </div>
      </div>
      <div class="generate-row">
        <div>{{$t('csr.saveInfo')}}</div>
        <large-button
          @click="generateCsr"
          :disabled="invalid || !disableDone"
        >{{$t('csr.generateCsr')}}</large-button>
      </div>
      <div class="button-footer">
        <div class="button-group">
          <large-button outlined @click="cancel" :disabled="!disableDone">{{$t('action.cancel')}}</large-button>
        </div>
        <large-button @click="done" :disabled="disableDone">{{$t('action.done')}}</large-button>
      </div>
    </ValidationObserver>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import { mapGetters } from 'vuex';
import LargeButton from '@/components/ui/LargeButton.vue';
import { ValidationProvider, ValidationObserver } from 'vee-validate';

export default Vue.extend({
  components: {
    LargeButton,
    ValidationObserver,
    ValidationProvider,
  },
  computed: {
    ...mapGetters(['csrForm']),
  },
  data() {
    return {
      disableDone: true,
    };
  },
  methods: {
    cancel(): void {
      this.$emit('cancel');
    },
    done(): void {
      this.$emit('done');
    },
    generateCsr(): void {
      this.$store.dispatch('generateCsr').then(
        (response) => {
          this.disableDone = false;
        },
        (error) => {
          this.$bus.$emit('show-error', error.message);
        },
      );
    },
  },
});
</script>

<style lang="scss" scoped>
@import '../../assets/colors';
@import '../../assets/shared';

.generate-row {
  margin-top: 40px;
  display: flex;
  flex-direction: row;
  align-items: baseline;
  justify-content: space-between;
}

.row-wrap {
  display: flex;
  flex-direction: row;
  align-items: baseline;
}

.label {
  width: 230px;
  display: flex;
  flex-direction: row;
  align-items: baseline;
}

.form-input {
  width: 300px;
}

.button-footer {
  display: flex;
  flex-direction: row;
  align-items: baseline;
  justify-content: space-between;
  border-top: solid 1px $XRoad-Grey40;
  margin-top: 40px;
  padding-top: 30px;
}

.button-group {
  display: flex;
  flex-direction: row;
  align-items: baseline;

  :not(:last-child) {
    margin-right: 20px;
  }
}
</style>

