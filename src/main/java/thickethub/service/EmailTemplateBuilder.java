package thickethub.service;

public class EmailTemplateBuilder {

    private static final String APP_URL = "https://www.thickethub.dev.br";

    public static String gerarHtmlNotificacaoChamado(
            String nomeDestinatario,
            String protocolo,
            String titulo,
            String status,
            String prioridade,
            String setor,
            String tecnicoNome,
            Long chamadoId,
            String mensagemDestaque
    ) {
        String statusFormatado = status != null ? status : "ABERTO";
        String prioridadeFormatada = prioridade != null ? prioridade : "MEDIA";
        String linkChamado = APP_URL + "/chamados/" + (chamadoId != null ? chamadoId : "");
        String nomeExibicao = (nomeDestinatario != null && !nomeDestinatario.isBlank()) ? nomeDestinatario : "Usuário";

        String badgeStatus = obterBadgeStatus(statusFormatado);
        String badgePrioridade = obterBadgePrioridade(prioridadeFormatada);
        String tecnicoExibicao = (tecnicoNome != null && !tecnicoNome.isBlank()) ? tecnicoNome : "Aguardando atribuição";

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>[ThicketHub] %s</title>
              <style>
                body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; color: #1e293b; -webkit-font-smoothing: antialiased; }
                table { border-collapse: collapse; }
                .wrapper { width: 100%%; table-layout: fixed; background-color: #f1f5f9; padding: 32px 0; }
                .main { background-color: #ffffff; margin: 0 auto; width: 100%%; max-width: 620px; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.08), 0 8px 10px -6px rgba(15, 23, 42, 0.04); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #0f172a 0%%, #1e293b 100%%); padding: 28px 36px; text-align: left; }
                .brand-title { color: #ffffff; font-size: 20px; font-weight: 800; letter-spacing: -0.5px; margin: 0; display: inline-block; }
                .brand-badge { background: rgba(59, 130, 246, 0.2); color: #60a5fa; border: 1px solid rgba(96, 165, 250, 0.3); font-size: 11px; font-weight: 700; text-transform: uppercase; padding: 3px 9px; border-radius: 9999px; vertical-align: middle; margin-left: 8px; }
                .brand-sub { color: #94a3b8; font-size: 13px; margin: 6px 0 0 0; }
                .content { padding: 36px; }
                .greeting { font-size: 16px; font-weight: 600; color: #0f172a; margin-top: 0; margin-bottom: 16px; }
                .alert-card { background: #f8fafc; border-left: 4px solid #2563eb; border-radius: 6px; padding: 16px 20px; margin-bottom: 28px; }
                .alert-text { margin: 0; font-size: 15px; color: #334155; line-height: 1.5; font-weight: 500; }
                .details-box { background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 20px 24px; margin-bottom: 30px; }
                .details-row { padding: 8px 0; border-bottom: 1px solid #f1f5f9; }
                .details-row:last-child { border-bottom: none; }
                .details-label { font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; color: #64748b; width: 35%%; }
                .details-val { font-size: 14px; font-weight: 600; color: #0f172a; }
                .protocol-badge { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; background: #e2e8f0; color: #0f172a; padding: 4px 10px; border-radius: 6px; font-size: 13px; font-weight: 700; }
                .btn-container { text-align: center; margin: 32px 0 20px 0; }
                .btn-cta { display: inline-block; background: linear-gradient(135deg, #2563eb 0%%, #1d4ed8 100%%); color: #ffffff !important; font-size: 15px; font-weight: 700; text-decoration: none; padding: 14px 32px; border-radius: 8px; box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35); }
                .footer { background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 24px 36px; text-align: center; font-size: 12px; color: #64748b; line-height: 1.6; }
                .footer p { margin: 4px 0; }
              </style>
            </head>
            <body>
              <div class="wrapper">
                <table class="main" role="presentation" align="center">
                  <tr>
                    <td class="header">
                      <div>
                        <span class="brand-title">ThicketHub</span>
                        <span class="brand-badge">Central de TI</span>
                      </div>
                      <p class="brand-sub">Sistema Integrado de Atendimento e Governança Corporativa</p>
                    </td>
                  </tr>
                  <tr>
                    <td class="content">
                      <h2 class="greeting">Olá, %s!</h2>
                      <div class="alert-card">
                        <p class="alert-text">%s</p>
                      </div>

                      <table class="details-box" width="100%%">
                        <tr class="details-row">
                          <td class="details-label">Protocolo</td>
                          <td class="details-val"><span class="protocol-badge">%s</span></td>
                        </tr>
                        <tr class="details-row">
                          <td class="details-label">Título</td>
                          <td class="details-val">%s</td>
                        </tr>
                        <tr class="details-row">
                          <td class="details-label">Status</td>
                          <td class="details-val">%s</td>
                        </tr>
                        <tr class="details-row">
                          <td class="details-label">Prioridade</td>
                          <td class="details-val">%s</td>
                        </tr>
                        <tr class="details-row">
                          <td class="details-label">Setor</td>
                          <td class="details-val">%s</td>
                        </tr>
                        <tr class="details-row">
                          <td class="details-label">Técnico</td>
                          <td class="details-val">%s</td>
                        </tr>
                      </table>

                      <div class="btn-container">
                        <a href="%s" class="btn-cta" target="_blank">Acompanhar Chamado na Central &rarr;</a>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td class="footer">
                      <p><strong>Central de Serviços ThicketHub</strong> &bull; Ambientes e Governança de TI</p>
                      <p>Este é um comunicado automático gerado pelo sistema. Por favor, não responda diretamente a este e-mail.</p>
                      <p style="color: #94a3b8; font-size: 11px; margin-top: 8px;">&copy; 2026 ThicketHub. Todos os direitos reservados.</p>
                    </td>
                  </tr>
                </table>
              </div>
            </body>
            </html>
            """.formatted(
                protocolo != null ? protocolo : "Atendimento",
                nomeExibicao,
                mensagemDestaque != null ? mensagemDestaque : "Houve uma atualização no seu chamado.",
                protocolo != null ? protocolo : "N/D",
                titulo != null ? titulo : "Chamado Sem Título",
                badgeStatus,
                badgePrioridade,
                setor != null ? setor : "Suporte Geral",
                tecnicoExibicao,
                linkChamado
        );
    }

    public static String gerarHtmlGenerico(String titulo, String mensagem, String botaoTexto, String botaoUrl) {
        String urlBotao = (botaoUrl != null && !botaoUrl.isBlank()) ? botaoUrl : APP_URL;
        String labelBotao = (botaoTexto != null && !botaoTexto.isBlank()) ? botaoTexto : "Acessar Plataforma";

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>%s</title>
              <style>
                body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; color: #1e293b; }
                .wrapper { width: 100%%; background-color: #f1f5f9; padding: 32px 0; }
                .main { background-color: #ffffff; margin: 0 auto; max-width: 620px; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.08); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #0f172a 0%%, #1e293b 100%%); padding: 28px 36px; }
                .brand-title { color: #ffffff; font-size: 20px; font-weight: 800; margin: 0; }
                .brand-sub { color: #94a3b8; font-size: 13px; margin: 4px 0 0 0; }
                .content { padding: 36px; }
                .title { font-size: 18px; font-weight: 700; color: #0f172a; margin-top: 0; margin-bottom: 16px; }
                .text { font-size: 15px; color: #334155; line-height: 1.6; white-space: pre-line; }
                .btn-container { text-align: center; margin: 32px 0 16px 0; }
                .btn-cta { display: inline-block; background: linear-gradient(135deg, #2563eb 0%%, #1d4ed8 100%%); color: #ffffff !important; font-size: 15px; font-weight: 700; text-decoration: none; padding: 14px 32px; border-radius: 8px; box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35); }
                .footer { background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 24px 36px; text-align: center; font-size: 12px; color: #64748b; line-height: 1.6; }
              </style>
            </head>
            <body>
              <div class="wrapper">
                <table class="main" role="presentation" align="center" width="100%%">
                  <tr>
                    <td class="header">
                      <div class="brand-title">ThicketHub</div>
                      <p class="brand-sub">Central de Atendimento e Governança de TI</p>
                    </td>
                  </tr>
                  <tr>
                    <td class="content">
                      <h2 class="title">%s</h2>
                      <div class="text">%s</div>
                      <div class="btn-container">
                        <a href="%s" class="btn-cta" target="_blank">%s &rarr;</a>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td class="footer">
                      <p><strong>Central de Atendimento Corporativa ThicketHub</strong></p>
                      <p>Notificação automática enviada pelo sistema. Não responda diretamente a este e-mail.</p>
                      <p style="color: #94a3b8; font-size: 11px;">&copy; 2026 ThicketHub. Todos os direitos reservados.</p>
                    </td>
                  </tr>
                </table>
              </div>
            </body>
            </html>
            """.formatted(
                titulo != null ? titulo : "ThicketHub",
                titulo != null ? titulo : "Notificação ThicketHub",
                mensagem != null ? mensagem : "",
                urlBotao,
                labelBotao
        );
    }

    private static String obterBadgeStatus(String status) {
        String s = status.toUpperCase();
        if (s.contains("ABERTO")) {
            return "<span style=\"display: inline-block; background: #dbeafe; color: #1e40af; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 700;\">Aberto</span>";
        } else if (s.contains("ANDAMENTO") || s.contains("ATENDIMENTO")) {
            return "<span style=\"display: inline-block; background: #fef3c7; color: #92400e; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 700;\">Em Atendimento</span>";
        } else if (s.contains("RESOLVIDO") || s.contains("CONCLUIDO") || s.contains("FECHADO")) {
            return "<span style=\"display: inline-block; background: #d1fae5; color: #065f46; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 700;\">Resolvido</span>";
        } else if (s.contains("CANCELADO")) {
            return "<span style=\"display: inline-block; background: #f1f5f9; color: #475569; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 700;\">Cancelado</span>";
        }
        return "<span style=\"display: inline-block; background: #f1f5f9; color: #334155; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 700;\">" + status + "</span>";
    }

    private static String obterBadgePrioridade(String prioridade) {
        String p = prioridade.toUpperCase();
        if (p.contains("CRITICA")) {
            return "<span style=\"display: inline-block; background: #fee2e2; color: #991b1b; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 700;\">Crítica</span>";
        } else if (p.contains("ALTA")) {
            return "<span style=\"display: inline-block; background: #ffedd5; color: #9a3412; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 700;\">Alta</span>";
        } else if (p.contains("MEDIA")) {
            return "<span style=\"display: inline-block; background: #fef9c3; color: #854d0e; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 700;\">Média</span>";
        } else if (p.contains("BAIXA")) {
            return "<span style=\"display: inline-block; background: #ecfdf5; color: #065f46; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 700;\">Baixa</span>";
        }
        return "<span style=\"display: inline-block; background: #f1f5f9; color: #334155; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 700;\">" + prioridade + "</span>";
    }
}
