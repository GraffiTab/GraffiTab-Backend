var popupMask;
var popupDialog;
var clientId;
var oauth2KeyName;
var redirect_uri;

function handleLogin() {
  var scopes = [];

  var auths = window.swaggerUi.api.authSchemes || window.swaggerUi.api.securityDefinitions;
  if (auths) {
    var key;
    var defs = auths;
    for (key in defs) {
      var auth = defs[key];
      if (auth.type === 'oauth2' && auth.scopes) {
        oauth2KeyName = key;
        var scope;
        if (Array.isArray(auth.scopes)) {
          // 1.2 support
          var i;
          for (i = 0; i < auth.scopes.length; i++) {
            scopes.push(auth.scopes[i]);
          }
        }
        else {
          // 2.0 support
          for (scope in auth.scopes) {
            scopes.push({scope: scope, description: auth.scopes[scope]});
          }
        }
      }
    }
  }

  popupDialog = $(
    [
      '<div class="modal fade show api-popup-dialog" id="credentials-modal show" tabindex="-1" role="dialog" aria-labelledby="credentials-modal-label" aria-hidden="false">',
        '<div class="modal-dialog">',
          '<div class="modal-content">',
            '<div class="modal-header">',
              '<h5 class="modal-title" id="credentials-modal-label">Select OAuth2.0 Scopes</h5>',
              '<button type="button" class="close api-popup-cancel" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>',
            '</div>',
            '<div class="modal-body">',
              '<p>Scopes are used to grant an application different levels of access to data on behalf of the end user. Each API may declare one or more scopes.</p>',
              '<div class="oauth2-details"></div>',
              '<form>',
                '<div class="api-popup-scopes">',
                  '<div class="scopes">',
                    '<strong>Scopes:</strong>',
                  '</div>',
                '</div>',
              '<form>',
              '<p class="error-msg"></p>',
            '</div>',
            '<div class="modal-footer">',
              '<div class="api-popup-actions">',
                '<button class="api-popup-cancel btn btn-sm btn-outline-secondary text-uppercase mr-2" type="button">Cancel</button>',
                '<button class="api-popup-authbtn btn btn-sm btn-primary text-uppercase" type="button">Authorize</button>',
              '</div>',
            '</div>',
          '</div>',
        '</div>',
      '</div>',
      '<div class="modal-backdrop fade show"></div>'].join(''));
  $(document.body).append(popupDialog);


  if(oauth2KeyName) {
    details = defs[oauth2KeyName];
    oauth_dets = popupDialog.find('.oauth2-details').empty();
    str = [
      '<h5>'+ oauth2KeyName +' ('+details.type+', '+details.flow+')</h5>',
      '<pre><code class="hljs rounded">Authorization URL: '+details.authorizationUrl+'</br>',
      'Flow: '+details.flow+'</code></pre>',
      '<div class="form-group">',
        '<label for="input_client_id">client_id</label>',
        '<input class="form-control form-control-sm" type="input" id="input_client_id">',
      '</div>',
      '<h6>Scopes:</h6>'
    ].join('')
    oauth_dets.append(str);
  }

  popup = popupDialog.find('.scopes').empty();
  for (i = 0; i < scopes.length; i++) {
    scope = scopes[i];
    str = '<button type="button" class="scope btn btn-sm btm-sm btn-outline-secondary mr-1" data-toggle-scope="' + scope.scope + '"><i class="fa fa-check mr-2 d-none"></i>' + scope.scope + '</button> ';
    popup.append(str);
  }

  popupDialog.find('scopes').click(function () {
    popupMask.hide();
    popupDialog.hide();
    popupDialog.empty();
    popupDialog = [];
  });

  popupDialog.find('[data-toggle-scope]').click(function () {
    $(this).toggleClass('btn-outline-secondary btn-outline-success');
    $(this).find('.fa-check').toggleClass('d-none d-inline');
  });

  popupDialog.find('button.api-popup-cancel').click(function () {
    popupMask.hide();
    popupDialog.hide();
    popupDialog.empty();
    popupDialog.remove();
    popupDialog = [];
  });

  popupDialog.find('#input_client_id').on('change', function () {
    clientId = this.value;
  });

  $('button.api-popup-authbtn').unbind();
  popupDialog.find('button.api-popup-authbtn').click(function () {
    popupMask.hide();
    popupDialog.hide();

    var authSchemes = window.swaggerUi.api.authSchemes;
    var host = window.location;
    var pathname = location.pathname.substring(0, location.pathname.lastIndexOf("/"));
    var defaultRedirectUrl = host.protocol + '//' + host.host + pathname + '/assets/o2c.html';
    var redirectUrl = window.oAuthRedirectUrl || defaultRedirectUrl;
    var url = null;

    for (var key in authSchemes) {
      if (authSchemes.hasOwnProperty(key)) {
        var flow = authSchemes[key].flow;

        if (authSchemes[key].type === 'oauth2' && flow && (flow === 'implicit' || flow === 'accessCode')) {
          var dets = authSchemes[key];
          url = dets.authorizationUrl + '?response_type=' + (flow === 'implicit' ? 'token' : 'code');
          window.swaggerUi.tokenName = dets.tokenName || 'access_token';
          window.swaggerUi.tokenUrl = (flow === 'accessCode' ? dets.tokenUrl : null);
        }
        else if (authSchemes[key].grantTypes) {
          // 1.2 support
          var o = authSchemes[key].grantTypes;
          for (var t in o) {
            if (o.hasOwnProperty(t) && t === 'implicit') {
              var dets = o[t];
              var ep = dets.loginEndpoint.url;
              url = dets.loginEndpoint.url + '?response_type=token';
              window.swaggerUi.tokenName = dets.tokenName;
            }
            else if (o.hasOwnProperty(t) && t === 'accessCode') {
              var dets = o[t];
              var ep = dets.tokenRequestEndpoint.url;
              url = dets.tokenRequestEndpoint.url + '?response_type=code';
              window.swaggerUi.tokenName = dets.tokenName;
            }
          }
        }
      }
    }
    var scopes = [];
    var o = $(".scopes:last .active");
    for (k = 0; k < o.length; k++) {
      var scope = $(o[k]).attr('data-toggle-scope');
      if (scopes.indexOf(scope) === -1)
        scopes.push(scope);
    }

    // Implicit auth recommends a state parameter.
    var state = Math.random();

    window.enabledScopes = scopes;

    redirect_uri = redirectUrl;

    url += '&redirect_uri=' + encodeURIComponent(redirectUrl);
    url += '&client_id=' + encodeURIComponent(clientId);
    url += '&scope=' + encodeURIComponent(scopes.join(' '));
    url += '&state=' + encodeURIComponent(state);

    window.open(url);
  });

  popupMask.show();
  popupDialog.show();
}

function handleLogout() {
  for (key in window.authorizations.authz) {
    window.authorizations.remove(key)
  }
  window.enabledScopes = null;
  var oauthBtn = $('.api-ic');
  oauthBtn.addClass('btn-outline-secondary');
  oauthBtn.removeClass('btn-success');
  oauthBtn.removeClass('btn-warning');

  oauthBtn.text('oauth');

  $('#input_apiKey').val('');
}

function initOAuth(opts) {
  var o = (opts || {});
  var errors = [];

  popupMask = (o.popupMask || $('#api-common-mask'));
  popupDialog = (o.popupDialog || $('.api-popup-dialog'));

  if (errors.length > 0) {
    console.log('auth unable initialize oauth: ' + errors);
    return;
  }

  $('pre code').each(function (i, e) {
    hljs.highlightBlock(e)
  });

  var oauthBtn = $('.api-ic');
  oauthBtn.unbind();
  oauthBtn.click(function (s) {
    if ($(s.target).hasClass('btn-outline-secondary'))
      handleLogin();
    else {
      handleLogout();
    }
  });
}

window.processOAuthCode = function processOAuthCode(data) {
  var params = {
    'client_id': clientId,
    'code': data.code,
    'grant_type': 'authorization_code',
    'redirect_uri': redirect_uri
  };
  $.ajax(
    {
      url: window.swaggerUi.tokenUrl,
      type: "POST",
      data: params,
      success: function (data, textStatus, jqXHR) {
        onOAuthComplete(data);
      },
      error: function (jqXHR, textStatus, errorThrown) {
        onOAuthComplete("");
      }
    });
};

window.onOAuthComplete = function onOAuthComplete(token) {
  if (token) {
    if (token.error) {
      var checkbox = $('input[type=checkbox],.secured')
      checkbox.each(function (pos) {
        checkbox[pos].checked = false;
      });
      alert(token.error);
    }
    else {
      var b = token[window.swaggerUi.tokenName];
      if (b) {
        // if all roles are satisfied
        var o = null;
        $.each($('.auth #api_information_panel'), function (k, v) {
          var children = v;
          if (children && children.childNodes) {
            var requiredScopes = [];
            $.each((children.childNodes), function (k1, v1) {
              var inner = v1.innerHTML;
              if (inner)
                requiredScopes.push(inner);
            });
            var diff = [];
            for (var i = 0; i < requiredScopes.length; i++) {
              var s = requiredScopes[i];
              if (window.enabledScopes && window.enabledScopes.indexOf(s) == -1) {
                diff.push(s);
              }
            }
            if (diff.length > 0) {
              o = v.parentNode;
              // sorry, not all scopes are satisfied
              $(o).find('.api-ic').addClass('btn-warning');
              $(o).find('.api-ic').removeClass('btn-outline-secondary');
              $(o).find('.api-ic').removeClass('btn-success');
            }
            else {
              o = v.parentNode;
              // all scopes are satisfied
              $(o).find('.api-ic').addClass('btn-success');
              $(o).find('.api-ic').removeClass('btn-outline-secondary');
              $(o).find('.api-ic').removeClass('btn-warning');
            }
          }
        });
        $('.api-ic').text('signout');
        $('#input_apiKey').val(b);
        window.swaggerUi.api.clientAuthorizations.add(oauth2KeyName, new SwaggerClient.ApiKeyAuthorization('Authorization', 'Bearer ' + b, 'header'));
      }
    }
  }
};
