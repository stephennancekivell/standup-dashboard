

var app = new Vue({
  el: '#vue',
  data: {
    template: '',
    newMember: ''
  },
  methods: {
    load: function(){
        httpGet('/members', function ok(data){
            Vue.set(app, 'template', data.responseText);
        });
    },
    onEnter: function(){
        console.log('enter', this.newMember);
        http('POST', '/members', this.newMember, this.load);
    }
  }
});

var standupNext = new Vue({
    el: '#standup-next',
    data: {
        nextMember: '',
        allMembers: [] //['1','2','3']
    },
    computed: {
        tailMembers: function(){
            return this.allMembers.slice(1);
        }
    },
    methods: {
        load: function(){
            httpGet('/members', function ok(data){
                console.log('data', data);
                Vue.set(standupNext, 'allMembers', data.json);
                Vue.set(standupNext, 'nextMember', data.json[0]);
            });
        },

    }
});

function httpGet(url, onSuccess, onError) {
    http('GET', url, undefined, onSuccess, onError);
}

function httpPost(url, onSuccess, onError) {
    http('POST', url, undefined, onSuccess, onError);
}

function http(method, url, requestBody, onSuccess, onError) {
    var xhr = new XMLHttpRequest();
    //xmlhttp.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    xhr.open(method, url);
    xhr.onload = function() {
        if (xhr.status === 200) {
            if (onSuccess) {
                try {
                    xhr.json = JSON.parse(xhr.responseText);
                } catch (e) {}
                onSuccess(xhr);
            }
        }
        else {
            if (onError){
                onError(xhr);
            }
        }
    };
    xhr.send(requestBody);
}

app.load();
standupNext.load();